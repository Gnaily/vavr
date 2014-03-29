/**                       ___ __          ,                   ___                                
 *  __ ___ _____  _______/  /  / ______  / \_   ______ ______/__/_____  ______  _______ _____    
 * /  '__/'  _  \/   ___/      \/   "__\/  _/__/ ____/'  ___/  /   "__\/   ,  \/   ___/'  "__\   
 * \__/  \______/\______\__/___/\______/\___/\_____/ \______\_/\______/\__/___/\______\______/.io
 * Licensed under the Apache License, Version 2.0. Copyright 2014 Daniel Dietrich.
 */
package javaslang.match;

import static javaslang.lang.Lang.require;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javaslang.exception.NonFatal;
import javaslang.lang.Invocations;
import javaslang.option.None;
import javaslang.option.Option;
import javaslang.option.Some;

/**
 * A better switch for Java. A Match is
 * <ul>
 * <li>an expression, i.e. the call of {@link #apply(Object)} results in a value. In fact it is a
 * Function.</li>
 * <li>able to match types</li>
 * <li>able to match values</li>
 * <li>lazily processes an object in the case of a match</li>
 * </ul>
 * 
 * See {@link Matchs} for convenience methods creating a matcher.
 *
 * @param <T> The result type of the Match expression.
 */
public class Match<T> implements Function<Object, T> {

	private List<Case<T>> cases = new ArrayList<>();

	/**
	 * Use this method to match by type S. Implementations of this method apply the given function
	 * to an object, if the object is of type S.
	 * 
	 * @param <S> type of the object to be matched
	 * @param function A SerializableFunction which is applied to a matched object.
	 * @return this, the current instance of Match.
	 * @throws IllegalStateException if function is null.
	 */
	public <S> Match<T> caze(SerializableFunction<S, T> function) {
		require(function != null, "function is null");
		cases.add(new Case<>(None.instance(), function));
		return this;
	}

	/**
	 * Use this method to match by value. Implementations of this method apply the given function to
	 * an object, if the object equals a prototype of type S.
	 * 
	 * @param <S> type of the prototype object
	 * @param prototype An object to be matched by equality.
	 * @param function A function which is applied to a matched object.
	 * @return this, the current instance of Match.
	 * @throws IllegalStateException if function is null.
	 */
	public <S> Match<T> caze(S prototype, SerializableFunction<S, T> function) {
		require(function != null, "function is null");
		cases.add(new Case<>(new Some<>(prototype), function));
		return this;
	}

	/**
	 * Applies an object to this matcher.
	 * 
	 * @param obj An object.
	 * @return The result when applying the given obj to the first matching case. If the case has a
	 *         consumer, the result is null, otherwise the result of the underlying function or
	 *         supplier.
	 * @throws MatchError if no Match case matches the given object.
	 * @throws NonFatal if an error occurs executing the matched case.
	 */
	@Override
	public T apply(Object obj) {
		for (Case<T> caze : cases) {
			if (caze.isApplicable(obj)) {
				return caze.apply(obj);
			}
		}
		throw new MatchError(obj);
	}

	/**
	 * Internal representation of a Match case.
	 * 
	 * @param <T> The same type as the return type of the Match a case belongs to.
	 */
	static class Case<T> {
		final Option<?> prototype;
		final SerializableFunction<?, T> function;
		final Class<?> parameterType;

		/**
		 * Constructs a Case.
		 * 
		 * @param prototype
		 * @param function
		 */
		Case(Option<?> prototype, SerializableFunction<?, T> function) {
			this.prototype = prototype;
			this.function = function;
			this.parameterType = Invocations.getLambdaSignature(function).getParameterTypes()[0];
		}

		/**
		 * Checks if the Match case represented by this Case can be applied to the given object. The
		 * null value is applicable, if the prototype is null. If no prototype is specified, a null
		 * obj is not applicable because the first occuring function would match otherwise, which
		 * wouldn't be correct in general.
		 * 
		 * @param obj An object, may be null.
		 * @return true, if prototype is None or prototype is Some(value) and value equals obj,
		 *         false otherwise.
		 */
		boolean isApplicable(Object obj) {
			final boolean isCompatible = obj == null
					|| parameterType.isAssignableFrom(obj.getClass());
			return isCompatible
					&& prototype.map(val -> val == obj || (val != null && val.equals(obj))).orElse(
							obj != null);
		}

		/**
		 * Apply the function of this Case to the given object.
		 * 
		 * @param obj An object.
		 * @return The result of function.apply(obj).
		 */
		@SuppressWarnings("unchecked")
		T apply(Object obj) {
			return ((SerializableFunction<Object, T>) function).apply(obj);
		}
	}

	/**
	 * A function which implements Serializable in order to obtain runtime type information about
	 * the lambda via {@link javaslang.lang.Invocations#getLambdaSignature(Serializable)}.
	 *
	 * @param <T> The parameter type of the function.
	 * @param <R> The return type of the function.
	 */
	@FunctionalInterface
	public static interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
	}

}
