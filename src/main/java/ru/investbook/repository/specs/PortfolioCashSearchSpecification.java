/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.repository.specs;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.PortfolioCashEntity;
import ru.investbook.entity.PortfolioCashEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import static ru.investbook.repository.specs.SpecificationHelper.*;


@RequiredArgsConstructor(staticName = "of")
public class PortfolioCashSearchSpecification implements Specification<PortfolioCashEntity> {
    private final String portfolio;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private final String currency;

    @Override
    public Predicate toPredicate(Root<PortfolioCashEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return Stream.of(
                        filterByPortfolioName(root, builder, PortfolioCashEntity_.portfolio, portfolio, query),
                        filterByDateFrom(root, builder, PortfolioCashEntity_.timestamp, dateFrom),
                        filterByDateTo(root, builder, PortfolioCashEntity_.timestamp, dateTo),
                        filterByCurrency(root, builder, PortfolioCashEntity_.currency, currency))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }
}
