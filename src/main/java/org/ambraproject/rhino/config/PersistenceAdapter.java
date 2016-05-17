package org.ambraproject.rhino.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Arrays;

public interface PersistenceAdapter<M, D> {

  public abstract Class<M> getModelClass();

  public abstract Class<D> getDataClass();

  public abstract D encode(M model);

  public abstract M decode(D data);

  public static <E extends Enum<E>> PersistenceAdapter<E, String> byEnumName(Class<E> enumType){
    ImmutableMap<String, E> enumsByName = Maps.uniqueIndex(Arrays.asList(enumType.getEnumConstants()), E::name);
    return new PersistenceAdapter<E, String>() {
      @Override
      public Class<E> getModelClass() {
        return enumType;
      }

      @Override
      public Class<String> getDataClass() {
        return String.class;
      }

      @Override
      public String encode(E model) {
        return model.name();
      }

      @Override
      public E decode(String data) {
        return enumsByName.get(data);
      }
    };
  }

}
