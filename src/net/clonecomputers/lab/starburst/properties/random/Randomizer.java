package net.clonecomputers.lab.starburst.properties.random;

import java.util.*;

import com.google.common.reflect.*;
import com.google.gson.*;

public abstract class Randomizer<T extends Number> {
	protected final TypeToken<T> type;
	protected final boolean isDiscrete;
	protected final Random r;
	protected final double min, max;
	
	public Randomizer(Class<T> c, Random r, double min, double max) {
		this(TypeToken.of(c), r, min, max);
	}
	
	public Randomizer(Random r, double min, double max) {
		this((TypeToken<T>)null, r, min, max);
	}
	
	public Randomizer(TypeToken<T> type, Random r, double min, double max) {
		if(type == null) type = new TypeToken<T>(getClass()){};
		this.type = type;
		if(type.isAssignableFrom(Number.class)) throw new IllegalStateException(type+" is not a specific subtype of number");
		//if(!type.unwrap().isAssignableFrom(byte.class)) throw new IllegalStateException(type+" does not appear to be a number");
		isDiscrete = !(type.isAssignableFrom(Double.class) || type.isAssignableFrom(Float.class));
		this.r = r;
		this.min = min;
		this.max = max;
	}
	
	/**
	 * whether this randomizer can randomize
	 * override if this should be false
	 * @return true
	 */
	public boolean canRandomize() {
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public T randomize() {
		Number n = randomize0();
		if(n == null) return null;
		if(type.isAssignableFrom(Double.class)) {
			return (T)new Double(n.doubleValue());
		}
		if(type.isAssignableFrom(Float.class)) {
			return (T)new Float(n.floatValue());
		}
		if(type.isAssignableFrom(Long.class)) {
			return (T)new Long(n.longValue());
		}
		if(type.isAssignableFrom(Integer.class)) {
			return (T)new Integer(n.intValue());
		}
		if(type.isAssignableFrom(Short.class)) {
			return (T)new Short(n.shortValue());
		}
		if(type.isAssignableFrom(Byte.class)) {
			return (T)new Byte(n.byteValue());
		}
		throw new IllegalStateException(type+" is not a number");
	}
	
	public static <T extends Number> Randomizer<T> createRandomizer(Class<T> datatype, Random r, double min, double max, JsonElement data) {
		if(data == null) {
			return new DefaultRandomizer<T>(datatype, r, min, max);
		}
		if(data.isJsonPrimitive() && !data.getAsBoolean()) {
			return new NoRandomizer<T>(datatype);
		}
		JsonObject dataObject = data.getAsJsonObject();
		if(!dataObject.has("type")) {
			return new DefaultRandomizer<T>(datatype, r, min, max);
		}
		String type = dataObject.get("type").getAsString();
		if(type.equals("biased")) {
			return new BiasedRandomizer<T>(datatype, r, min, max, dataObject.get("bias").getAsDouble());
		}
		if(type.equals("gaussian")) {
			return new GaussianRandomizer<T>(datatype, r, min, max, 
					dataObject.get("mean").getAsDouble(), dataObject.get("variance").getAsDouble());
		}
		if(type.equals("flat")) {
			return new FlatRandomizer<T>(datatype, r, min, max);
		}
		if(type.equals("default")) {
			return new DefaultRandomizer<T>(datatype, r, min, max);
		}
		throw new IllegalArgumentException("Invalid type");
	}
	
	protected abstract Number randomize0();
}
