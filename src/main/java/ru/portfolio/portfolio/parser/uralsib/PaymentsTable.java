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

package ru.portfolio.portfolio.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.parser.uralsib.PortfolioSecuritiesTable.ReportSecurityInformation;
import ru.portfolio.portfolio.pojo.Security;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.parser.uralsib.PaymentsTable.PaymentsTableHeader.DESCRIPTION;

@Slf4j
abstract class PaymentsTable<RowType> extends AbstractReportTable<RowType> {

    static final String TABLE_NAME = "ДВИЖЕНИЕ ДЕНЕЖНЫХ СРЕДСТВ ЗА ОТЧЕТНЫЙ ПЕРИОД";
    // human readable name -> incoming count
    private final List<ReportSecurityInformation> securitiesIncomingCount;
    private final List<SecurityTransaction> securityTransactions;

    public PaymentsTable(UralsibBrokerReport report,
                         PortfolioSecuritiesTable securitiesTable,
                         SecurityTransactionTable securityTransactionTable) {
        super(report, TABLE_NAME, "", PaymentsTableHeader.class);
        this.securitiesIncomingCount = securitiesTable.getData();
        this.securityTransactions = securityTransactionTable.getData();
    }

    protected Security getSecurity(ExcelTable table, Row row) {
        String description = table.getStringCellValue(row, DESCRIPTION);
        String descriptionLowercase = description.toLowerCase();
        for (ReportSecurityInformation info : securitiesIncomingCount) {
            if (info == null) continue;
            Security security = info.getSecurity();
            if (contains(descriptionLowercase, info.getCfi()) ||   // dividend
                    (security != null && (contains(descriptionLowercase, security.getName()) ||  // coupon, amortization, redemption
                            contains(descriptionLowercase, security.getIsin())))) { // for furute report changes
                return security;
            }
        }
        throw new RuntimeException("Не могу найти ISIN ценной бумаги в отчете брокера по событию:" + description);
    }

    private boolean contains(String description, String securityParameter) {
        return securityParameter != null && description.contains(securityParameter.toLowerCase());
    }

    protected Integer getSecurityCount(Security security, Instant atInstant) {
        int count = securitiesIncomingCount.stream()
                .filter(i -> i.getSecurity().getIsin().equals(security.getIsin()))
                .map(ReportSecurityInformation::getIncomingCount)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Не найдено количество на начало периода отчета для ЦБ " + security));
        Collection<SecurityTransaction> transactions = securityTransactions.stream()
                .filter(t -> t.getIsin().equals(security.getIsin()))
                .sorted(Comparator.comparing(SecurityTransaction::getTimestamp))
                .collect(Collectors.toList());
        for (SecurityTransaction transaction : transactions) {
            if (transaction.getTimestamp().isBefore(atInstant)) {
                count += transaction.getCount();
            } else {
                break;
            }
        }
        return count;
    }

    enum PaymentsTableHeader implements TableColumnDescription {
        DATE("дата"),
        OPERATION("тип", "операции"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        DESCRIPTION("комментарий");

        @Getter
        private final TableColumn column;

        PaymentsTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
