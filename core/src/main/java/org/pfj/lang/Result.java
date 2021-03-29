/*
 * Copyright (c) 2021 Sergiy Yevtushenko.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.lang;

import org.pfj.lang.Functions.FN1;
import org.pfj.lang.Functions.FN2;
import org.pfj.lang.Functions.FN3;
import org.pfj.lang.Functions.FN4;
import org.pfj.lang.Functions.FN5;
import org.pfj.lang.Functions.FN6;
import org.pfj.lang.Functions.FN7;
import org.pfj.lang.Functions.FN8;
import org.pfj.lang.Functions.FN9;
import org.pfj.lang.Tuple.Tuple1;
import org.pfj.lang.Tuple.Tuple2;
import org.pfj.lang.Tuple.Tuple3;
import org.pfj.lang.Tuple.Tuple4;
import org.pfj.lang.Tuple.Tuple5;
import org.pfj.lang.Tuple.Tuple6;
import org.pfj.lang.Tuple.Tuple7;
import org.pfj.lang.Tuple.Tuple8;
import org.pfj.lang.Tuple.Tuple9;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.pfj.lang.Tuple.tuple;


/**
 * Representation of the operation result. The result can be either success or failure.
 * In case of success it holds value returned by the operation. In case of failure it
 * holds a failure description.
 *
 * @param <T> Type of value in case of success.
 */
public interface Result<T> {
	/**
	 * Transform operation result value into value of other type and wrap new
	 * value into {@link Result}. Transformation takes place if current instance
	 * (this) contains successful result, otherwise current instance remains
	 * unchanged and transformation function is not invoked.
	 *
	 * @param mapper Function to transform successful value
	 *
	 * @return transformed value (in case of success) or current instance (in case of failure)
	 */
	@SuppressWarnings("unchecked")
	default <R> Result<R> map(FN1<R, ? super T> mapper) {
		return reduce(l -> (Result<R>) this, r -> ok(mapper.apply(r)));
	}

	/**
	 * Transform operation result into another operation result. In case if current
	 * instance (this) is an error, transformation function is not invoked
	 * and value remains the same.
	 *
	 * @param mapper Function to apply to result
	 *
	 * @return transformed value (in case of success) or current instance (in case of failure)
	 */
	@SuppressWarnings("unchecked")
	default <R> Result<R> flatMap(FN1<Result<R>, ? super T> mapper) {
		return reduce(t -> (Result<R>) this, mapper);
	}

	/**
	 * Apply consumers to result value. Note that depending on the result (success or failure) only one consumer will be
	 * applied at a time.
	 *
	 * @param failureConsumer Consumer for failure result
	 * @param successConsumer Consumer for success result
	 *
	 * @return current instance
	 */
	default Result<T> apply(Consumer<? super Failure> failureConsumer, Consumer<? super T> successConsumer) {
		return reduce(t -> {
			failureConsumer.accept(t);
			return this;
		}, t -> {
			successConsumer.accept(t);
			return this;
		});
	}

	/**
	 * Combine current instance with another result. If current instance holds
	 * success then result is equivalent to current instance, otherwise other
	 * instance (passed as {@code replacement} parameter) is returned.
	 *
	 * @param replacement Value to return if current instance contains failure operation result
	 *
	 * @return current instance in case of success or replacement instance in case of failure.
	 */
	default Result<T> or(Result<T> replacement) {
		return reduce(t -> replacement, t -> this);
	}

	/**
	 * Combine current instance with another result. If current instance holds
	 * success then result is equivalent to current instance, otherwise instance provided by
	 * specified supplier is returned.
	 *
	 * @param supplier Supplier for replacement instance if current instance contains failure operation result
	 *
	 * @return current instance in case of success or result returned by supplier in case of failure.
	 */
	default Result<T> or(Supplier<Result<T>> supplier) {
		return reduce(t -> supplier.get(), t -> this);
	}

	/**
	 * Pass successful operation result value into provided consumer.
	 *
	 * @param consumer Consumer to pass value to
	 *
	 * @return current instance for fluent call chaining
	 */
	default Result<T> onSuccess(Consumer<T> consumer) {
		reduce(v -> null, v -> {
			consumer.accept(v);
			return null;
		});
		return this;
	}

	/**
	 * Run provided action in case of success.
	 *
	 * @return current instance for fluent call chaining
	 */
	default Result<T> onSuccessDo(Runnable action) {
		reduce(v -> null, v -> {
			action.run();
			return null;
		});
		return this;
	}

	/**
	 * Pass failure operation result value into provided consumer.
	 *
	 * @param consumer Consumer to pass value to
	 *
	 * @return current instance for fluent call chaining
	 */
	default Result<T> onFailure(Consumer<? super Failure> consumer) {
		reduce(v -> {
			consumer.accept(v);
			return null;
		}, v -> null);
		return this;
	}

	/**
	 * Run provided action in case of failure.
	 *
	 * @return current instance for fluent call chaining
	 */
	default Result<T> onFailureDo(Runnable action) {
		reduce(v -> {
			action.run();
			return null;
		}, v -> null);
		return this;
	}

	/**
	 * Convert instance into {@link Option} of the same value type. Successful instance
	 * is converted into present {@link Option} and failure - into empty {@link Option}.
	 * Note that during such a conversion error information is get lost.
	 *
	 * @return {@link Option} instance which is present in case of success and missing
	 * 	in case of failure.
	 */
	default Option<T> toOption() {
		return reduce(t1 -> Option.empty(), Option::option);
	}

	/**
	 * Convert instance into {@link Optional} of the same value type. Successful instance
	 * is converted into present {@link Optional} and failure - into empty {@link Optional}.
	 * Note that during such a conversion error information is get lost.
	 *
	 * @return {@link Optional} instance which is present in case of success and missing
	 * 	in case of failure.
	 */
	default Optional<T> toOptional() {
		return reduce(t1 -> Optional.empty(), Optional::of);
	}

	/**
	 * Handle both possible states (success/failure) and produce single value from it.
	 *
	 * @param failureMapper function to transform failure into value
	 * @param successMapper function to transform success into value
	 *
	 * @return result of application of one of the mappers.
	 */
	<R> R reduce(FN1<? extends R, ? super Failure> failureMapper, FN1<? extends R, ? super T> successMapper);

	/**
	 * Create an instance of successful operation result.
	 *
	 * @param value Operation result
	 *
	 * @return created instance
	 */
	static <R> Result<R> ok(R value) {
		return new Result<R>() {
			@Override
			public <T> T reduce(FN1<? extends T, ? super Failure> failureMapper, FN1<? extends T, ? super R> successMapper) {
				return successMapper.apply(value);
			}

			@Override
			public int hashCode() {
				return Objects.hash(reduce(FN1.id(), FN1.id()));
			}

			@Override
			public boolean equals(Object obj) {
				return (obj instanceof Result<?> result) && Result.equals(this, result);
			}

			@Override
			public String toString() {
				return "Result(" + value.toString() + ")";
			}
		};
	}

	/**
	 * Create an instance of failure operation result.
	 *
	 * @param value Operation error value
	 *
	 * @return created instance
	 */
	static <R> Result<R> fail(Failure value) {
		return new Result<R>() {
			@Override
			public <T> T reduce(FN1<? extends T, ? super Failure> failureMapper, FN1<? extends T, ? super R> successMapper) {
				return failureMapper.apply(value);
			}

			@Override
			public int hashCode() {
				return value.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				return (obj instanceof Result<?> result) && Result.equals(this, result);
			}

			@Override
			public String toString() {
				return "Result(" + value + ")";
			}
		};
	}

	/**
	 * Create an instance of failure operation result.
	 *
	 * @param message failure message.
	 *
	 * @return created instance
	 */
	static <R> Result<R> fail(String message) {
		return fail(Failure.failure(message));
	}

	/**
	 * Create an instance of failure operation result.
	 *
	 * @param format failure message format.
	 * @param params failure message parameters.
	 *
	 * @return created instance
	 *
	 * @see java.text.MessageFormat for message format details.
	 */
	static <R> Result<R> fail(String format, Object... params) {
		return fail(Failure.failure(format, params));
	}

	static boolean equals(Result<?> one, Result<?> two) {
		if (one == two) {
			return true;
		}

		return one.reduce(
			v -> Objects.equals(v, two.reduce(FN1.id(), v1 -> null)),
			v -> Objects.equals(v, two.reduce(v1 -> null, FN1.id()))
		);
	}

	static <T> Result<List<T>> flatten(List<Result<T>> resultList) {
		var failure = new Failure[1];
		var values = new ArrayList<T>();

		resultList.forEach(val -> val.reduce(f -> failure[0] = f, values::add));

		return failure[0] != null ? Result.fail(failure[0])
								  : Result.ok(values);
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper1} prepared for further transformation.
	 */
	static <T1> Mapper1<T1> all(Result<T1> value) {
		return () -> value.flatMap(vv1 -> ok(tuple(vv1)));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper2} prepared for further transformation.
	 */
	static <T1, T2> Mapper2<T1, T2> all(Result<T1> value1, Result<T2> value2) {
		return () -> value1.flatMap(vv1 -> value2.flatMap(vv2 -> ok(tuple(vv1, vv2))));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper3} prepared for further transformation.
	 */
	static <T1, T2, T3> Mapper3<T1, T2, T3> all(Result<T1> value1, Result<T2> value2, Result<T3> value3) {
		return () -> value1.flatMap(vv1 -> value2.flatMap(vv2 -> value3.flatMap(vv3 -> ok(tuple(vv1, vv2, vv3)))));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper4} prepared for further transformation.
	 */
	static <T1, T2, T3, T4> Mapper4<T1, T2, T3, T4> all(
		Result<T1> value1, Result<T2> value2, Result<T3> value3, Result<T4> value4
	) {
		return () -> value1.flatMap(
			vv1 -> value2.flatMap(
				vv2 -> value3.flatMap(
					vv3 -> value4.flatMap(
						vv4 -> ok(tuple(vv1, vv2, vv3, vv4))))));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper5} prepared for further transformation.
	 */
	static <T1, T2, T3, T4, T5> Mapper5<T1, T2, T3, T4, T5> all(
		Result<T1> value1, Result<T2> value2, Result<T3> value3, Result<T4> value4, Result<T5> value5
	) {
		return () -> value1.flatMap(
			vv1 -> value2.flatMap(
				vv2 -> value3.flatMap(
					vv3 -> value4.flatMap(
						vv4 -> value5.flatMap(
							vv5 -> ok(tuple(vv1, vv2, vv3, vv4, vv5)))))));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper6} prepared for further transformation.
	 */
	static <T1, T2, T3, T4, T5, T6> Mapper6<T1, T2, T3, T4, T5, T6> all(
		Result<T1> value1, Result<T2> value2, Result<T3> value3,
		Result<T4> value4, Result<T5> value5, Result<T6> value6
	) {
		return () -> value1.flatMap(
			vv1 -> value2.flatMap(
				vv2 -> value3.flatMap(
					vv3 -> value4.flatMap(
						vv4 -> value5.flatMap(
							vv5 -> value6.flatMap(
								vv6 -> ok(tuple(vv1, vv2, vv3, vv4, vv5, vv6))))))));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper7} prepared for further transformation.
	 */
	static <T1, T2, T3, T4, T5, T6, T7> Mapper7<T1, T2, T3, T4, T5, T6, T7> all(
		Result<T1> value1, Result<T2> value2, Result<T3> value3,
		Result<T4> value4, Result<T5> value5, Result<T6> value6,
		Result<T7> value7
	) {
		return () -> value1.flatMap(
			vv1 -> value2.flatMap(
				vv2 -> value3.flatMap(
					vv3 -> value4.flatMap(
						vv4 -> value5.flatMap(
							vv5 -> value6.flatMap(
								vv6 -> value7.flatMap(
									vv7 -> ok(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7)))))))));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper8} prepared for further transformation.
	 */
	static <T1, T2, T3, T4, T5, T6, T7, T8> Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> all(
		Result<T1> value1, Result<T2> value2, Result<T3> value3,
		Result<T4> value4, Result<T5> value5, Result<T6> value6,
		Result<T7> value7, Result<T8> value8
	) {
		return () -> value1.flatMap(
			vv1 -> value2.flatMap(
				vv2 -> value3.flatMap(
					vv3 -> value4.flatMap(
						vv4 -> value5.flatMap(
							vv5 -> value6.flatMap(
								vv6 -> value7.flatMap(
									vv7 -> value8.flatMap(
										vv8 -> ok(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7, vv8))))))))));
	}

	/**
	 * Transform provided results into single result containing tuple of values. The result is failure
	 * if any input result is failure. Otherwise returned instance contains tuple with values from input results.
	 *
	 * @return {@link Mapper9} prepared for further transformation.
	 */
	static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> all(
		Result<T1> value1, Result<T2> value2, Result<T3> value3,
		Result<T4> value4, Result<T5> value5, Result<T6> value6,
		Result<T7> value7, Result<T8> value8, Result<T9> value9
	) {
		return () -> value1.flatMap(
			vv1 -> value2.flatMap(
				vv2 -> value3.flatMap(
					vv3 -> value4.flatMap(
						vv4 -> value5.flatMap(
							vv5 -> value6.flatMap(
								vv6 -> value7.flatMap(
									vv7 -> value8.flatMap(
										vv8 -> value9.flatMap(
											vv9 -> ok(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7, vv8, vv9)))))))))));
	}

	/**
	 * Helper interface for convenient {@link Tuple1} transformation.
	 */
	interface Mapper1<T1> {
		Result<Tuple1<T1>> id();

		default <R> Result<R> map(FN1<R, T1> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN1<Result<R>, T1> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple2} transformation.
	 */
	interface Mapper2<T1, T2> {
		Result<Tuple2<T1, T2>> id();

		default <R> Result<R> map(FN2<R, T1, T2> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN2<Result<R>, T1, T2> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple3} transformation.
	 */
	interface Mapper3<T1, T2, T3> {
		Result<Tuple3<T1, T2, T3>> id();

		default <R> Result<R> map(FN3<R, T1, T2, T3> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN3<Result<R>, T1, T2, T3> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple4} transformation.
	 */
	interface Mapper4<T1, T2, T3, T4> {
		Result<Tuple4<T1, T2, T3, T4>> id();

		default <R> Result<R> map(FN4<R, T1, T2, T3, T4> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN4<Result<R>, T1, T2, T3, T4> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple5} transformation.
	 */
	interface Mapper5<T1, T2, T3, T4, T5> {
		Result<Tuple5<T1, T2, T3, T4, T5>> id();

		default <R> Result<R> map(FN5<R, T1, T2, T3, T4, T5> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN5<Result<R>, T1, T2, T3, T4, T5> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple6} transformation.
	 */
	interface Mapper6<T1, T2, T3, T4, T5, T6> {
		Result<Tuple6<T1, T2, T3, T4, T5, T6>> id();

		default <R> Result<R> map(FN6<R, T1, T2, T3, T4, T5, T6> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN6<Result<R>, T1, T2, T3, T4, T5, T6> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple7} transformation.
	 */
	interface Mapper7<T1, T2, T3, T4, T5, T6, T7> {
		Result<Tuple7<T1, T2, T3, T4, T5, T6, T7>> id();

		default <R> Result<R> map(FN7<R, T1, T2, T3, T4, T5, T6, T7> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN7<Result<R>, T1, T2, T3, T4, T5, T6, T7> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple8} transformation.
	 */
	interface Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> {
		Result<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> id();

		default <R> Result<R> map(FN8<R, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN8<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}

	/**
	 * Helper interface for convenient {@link Tuple9} transformation.
	 */
	interface Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
		Result<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> id();

		default <R> Result<R> map(FN9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
			return id().map(tuple -> tuple.map(mapper));
		}

		default <R> Result<R> flatMap(FN9<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
			return id().flatMap(tuple -> tuple.map(mapper));
		}
	}
}
