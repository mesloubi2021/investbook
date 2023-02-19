/*
 * InvestBook
 * Copyright (C) 2023  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.uralsib;

import ru.investbook.parser.TransactionValueAndFeeParser;

public class RepoTransactionTable extends SecurityTransactionTable {
    private static final String TABLE_NAME = "Специальные сделки РЕПО для переноса длинной позиции";

    protected RepoTransactionTable(UralsibBrokerReport report,
                                   SecuritiesTable securitiesTable,
                                   ForeignExchangeRateTable foreignExchangeRateTable,
                                   TransactionValueAndFeeParser transactionValueAndFeeParser) {
        super(report, TABLE_NAME, securitiesTable, foreignExchangeRateTable, transactionValueAndFeeParser);
    }
}
