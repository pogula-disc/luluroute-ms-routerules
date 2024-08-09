package com.luluroute.ms.routerules.business.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ObjectMapperUtil {

	private static ModelMapper modelMapper;

	static {
		modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
	}

	public static <D, T> D map(final T source, Class<D> resultClass) {
		return modelMapper.map(source, resultClass);
	}

	public static <D, T> List<D> mapAll(final Collection<T> sourceList, Class<D> resultClass) {
		return sourceList.stream().map(source -> map(source, resultClass)).collect(Collectors.toList());
	}

	public static <D, T> List<D> mapIterator(final Iterable<T> sourceIterator, Class<D> resultClass) {
		return StreamSupport.stream(sourceIterator.spliterator(), false).map(source -> map(source, resultClass))
				.collect(Collectors.toList());
	}

	public static <S, D> D map(final S source, D destination) {
		modelMapper.map(source, destination);
		return destination;
	}

	public static <I, D, T> D map(I id, final T source, Class<D> resultClass) {
		return modelMapper.map(source, resultClass);
	}

}
