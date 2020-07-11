package org.orienteer.jnpm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Maybe;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * {@link CallAdapter} to workaround NPM registry error codes
 * @param <R> return type
 */
class JNPMCallAdapter<R> implements CallAdapter<R, Object> {
	
	private final CallAdapter<?, ?> delegate;
	private final Class<?> returnClass;
	
	public JNPMCallAdapter(CallAdapter<?, ?> delegate, Class<?> returnClass) {
		this.delegate = delegate;
		this.returnClass = returnClass;
	}

	@Override
	public Type responseType() {
		return delegate.responseType();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object adapt(Call<R> call) {
		Object ret = delegate.adapt((Call)call);
		if(ret instanceof Maybe) {
			ret = ((Maybe<?>)ret)
					.onErrorComplete(t -> t instanceof HttpException 
							&& (((HttpException)t).code() == 404 || ((HttpException)t).code() == 405));
		}
		return ret;
	}
	
	static class JNPMCallAdapterFactory extends Factory {
		
		private RxJava2CallAdapterFactory delegate;
		
		private JNPMCallAdapterFactory(RxJava2CallAdapterFactory delegate) {
			this.delegate = delegate;
		}
		
		public static JNPMCallAdapterFactory create(RxJava2CallAdapterFactory delegate) {
			return new JNPMCallAdapterFactory(delegate);
		}

		@Override
		public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
			CallAdapter<?, ?> callAdapter = delegate.get(returnType, annotations, retrofit);
			if(callAdapter==null) return null;
			return callAdapter==null?null:new JNPMCallAdapter<Object>(callAdapter, getRawType(returnType));
		}
		
	}

}
