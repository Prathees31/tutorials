package org.baeldung.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.baeldung.web.util.SearchOperation;
import org.baeldung.web.util.SpecSearchCriteria;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;

public class GenericSpecificationsBuilder {

	private final List<SpecSearchCriteria> params;

	public GenericSpecificationsBuilder() {
		this.params = new ArrayList<>();
	}

	public final GenericSpecificationsBuilder with(final String key, final String operation, final Object value,
			final String prefix, final String suffix) {
		return with(null, key, operation, value, prefix, suffix);
	}

	public final GenericSpecificationsBuilder with(final String precedenceIndicator, final String key,
			final String operation, final Object value, final String prefix, final String suffix) {
		SearchOperation op = SearchOperation.getSimpleOperation(operation.charAt(0));
		if (op != null) {
			if (op == SearchOperation.EQUALITY) // the operation may be complex operation
			{
				final boolean startWithAsterisk = prefix != null && prefix.contains(SearchOperation.ZERO_OR_MORE_REGEX);
				final boolean endWithAsterisk = suffix != null && suffix.contains(SearchOperation.ZERO_OR_MORE_REGEX);

				if (startWithAsterisk && endWithAsterisk) {
					op = SearchOperation.CONTAINS;
				} else if (startWithAsterisk) {
					op = SearchOperation.ENDS_WITH;
				} else if (endWithAsterisk) {
					op = SearchOperation.STARTS_WITH;
				}
			}
			params.add(new SpecSearchCriteria(precedenceIndicator, key, op, value));
		}
		return this;
	}

	public <U> Specification<U> build(Function<SpecSearchCriteria, Specification<U>> converter) {

		if (params.size() == 0)
			return null;
		
		params.sort((spec0, spec1) -> Boolean.compare(spec0.isLowPrecedence(), spec1.isLowPrecedence()));
		
		final List<Specification<U>> specs  = params.stream().map(converter).collect(Collectors.toCollection(ArrayList::new));
		
		Specification<U> result = specs.get(0);
		
		for (int idx = 1; idx < specs.size(); idx++) {
			result=params.get(idx).isLowPrecedence()? Specifications.where(result).or(specs.get(idx)): Specifications.where(result).and(specs.get(idx));
		}
		return result;
	}
}
