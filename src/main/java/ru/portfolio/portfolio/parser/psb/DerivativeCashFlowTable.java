/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.parser.psb;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.*;

@Slf4j
public class DerivativeCashFlowTable extends AbstractReportTable<SecurityEventCashFlow> {

    private static final String TABLE1_NAME = "Прочие операции";
    private static final String TABLE2_NAME = "Движение стандартных контрактов";
    private static final String TABLE_END_TEXT = "Итого";
    @Getter(AccessLevel.PRIVATE)
    private Map<String, Integer> contractCount = Collections.emptyMap();

    public DerivativeCashFlowTable(PsbBrokerReport report) {
        super(report, TABLE1_NAME, TABLE_END_TEXT, DerivativeCashFlowTableHeader.class);
    }

    @Override
    protected Collection<SecurityEventCashFlow> parseTable(ExcelTable table) {
        return hasOpenContract() ? super.parseTable(table) : Collections.emptyList();
    }

    private boolean hasOpenContract() {
        ExcelTable countTable = ExcelTable.of(getReport().getSheet(), TABLE2_NAME, TABLE_END_TEXT, ContractCountTableHeader.class);
        List<AbstractMap.SimpleEntry<String, Integer>> counts = countTable.getData(getReport().getPath(), DerivativeCashFlowTable::getCount);
        this.contractCount = counts.stream()
                .filter(e -> e.getValue() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return !this.contractCount.isEmpty();
    }

    private static AbstractMap.SimpleEntry<String, Integer> getCount(ExcelTable table, Row row) {
        String contract = table.getStringCellValue(row, CONTRACT);
        int incomingCount = Math.abs(table.getIntCellValue(row, INCOUMING));
        int outgoingCount = Math.abs(table.getIntCellValue(row, OUTGOING));
        int count = Math.max(incomingCount, outgoingCount);
        if (count == 0) {
            count = Math.abs(table.getIntCellValue(row, BUY)); // buyCount == cellCount
        }
        return new AbstractMap.SimpleEntry<>(contract, count);
    }

    @Override
    protected Collection<SecurityEventCashFlow> getRow(ExcelTable table, Row row) {
        BigDecimal value = table.getCurrencyCellValue(row, DerivativeCashFlowTableHeader.INCOUMING)
                .subtract(table.getCurrencyCellValue(row, DerivativeCashFlowTableHeader.OUTGOING));
        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DerivativeCashFlowTableHeader.DATE)))
                .portfolio(getReport().getPortfolio())
                .value(value)
                .currency("RUB"); // FORTS, only RUB
        String action = table.getCell(row, DerivativeCashFlowTableHeader.OPERATION).getStringCellValue().toLowerCase();
        switch (action) {
            case "вариационная маржа":
                String contract = table.getStringCellValue(row, DerivativeCashFlowTableHeader.CONTRACT)
                        .split("/")[1].trim();
                Integer count = getContractCount().get(contract);
                if (count == null) {
                    throw new IllegalArgumentException("Открытых контрактов не найдено");
                }
                return singletonList(builder.eventType(CashFlowType.DERIVATIVE_PROFIT)
                        .isin(contract)
                        .count(count)
                        .build());
            case "биржевой сбор":
                return emptyList(); // изменения отображаются в ликвидной стоимости портфеля
            case "заблокированo / разблокировано средств под го":
                return emptyList(); // не влияет на размер собственных денежных средств
            default:
                throw new IllegalArgumentException("Неизвестный вид операции " + action);
        }
    }

    enum ContractCountTableHeader implements TableColumnDescription {
        CONTRACT("контракт"),
        INCOUMING("входящий остаток"),
        OUTGOING("исходящий остаток"),
        BUY("зачислено"),
        CELL("списано");

        @Getter
        private final TableColumn column;
        ContractCountTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

    enum DerivativeCashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        CONTRACT("№", "контракт"),
        OPERATION("вид операции"),
        INCOUMING("зачислено"),
        OUTGOING("списано");

        @Getter
        private final TableColumn column;
        DerivativeCashFlowTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
