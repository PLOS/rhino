/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.config;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.TypeResolver;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class HibernateAdaptingType<M, D> implements UserType, ParameterizedType {

  private static final String CLASS_PROPERTY_NAME = "class";
  private static final String ADAPTER_PROPERTY_NAME = "adapter";

  private PersistenceAdapter<M, D> adapter;

  @Override
  public void setParameterValues(Properties parameters) {
    String className = parameters.getProperty(CLASS_PROPERTY_NAME);
    String adapterFieldName = parameters.getProperty(ADAPTER_PROPERTY_NAME);
    try {
      Class<?> classWithAdapter = Class.forName(className);
      Field adapterField = classWithAdapter.getDeclaredField(adapterFieldName);
      this.adapter = (PersistenceAdapter<M, D>) adapterField.get(null);
    } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
      String message = String.format("Error accessing PersistenceAdapter at %s.%s", className, adapterFieldName);
      throw new RuntimeException(message, e);
    }
  }

  private AbstractSingleColumnStandardBasicType<?> getDataType() {
    return (AbstractSingleColumnStandardBasicType<?>) new TypeResolver().basic(adapter.getDataClass().getName());
  }

  @Override
  public int[] sqlTypes() {
    return new int[]{getDataType().sqlType()};
  }

  @Override
  public Class returnedClass() {
    return adapter.getModelClass();
  }

  @Override
  public boolean equals(Object x, Object y) {
    return Objects.equals(x, y);
  }

  @Override
  public int hashCode(Object x) {
    return (x == null) ? 0 : x.hashCode();
  }

  @Override
  public M nullSafeGet(ResultSet rs, String[] names, Object owner) throws SQLException {
    final AbstractSingleColumnStandardBasicType<?> dataType = getDataType();
    final Class<D> dataClass = adapter.getDataClass();

    D data = null;
    for (int i = 0; (data == null) && (i < names.length); i++) {
      data = dataClass.cast(dataType.get(rs, names[i]));
    }
    return (data == null) ? null : adapter.decode(data);
  }

  @Override
  public void nullSafeSet(PreparedStatement st, Object value, int index) throws SQLException {
    M model = adapter.getModelClass().cast(value);
    D data = (model == null) ? null : adapter.encode(model);
    st.setObject(index, data);
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Object deepCopy(Object value) {
    return value;
  }

  @Override
  public Serializable disassemble(Object value) {
    return (Serializable) value;
  }

  @Override
  public Object assemble(Serializable cached, Object owner) {
    return cached;
  }

  @Override
  public Object replace(Object original, Object target, Object owner) {
    return original;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HibernateAdaptingType<?, ?> that = (HibernateAdaptingType<?, ?>) o;
    return adapter != null ? adapter.equals(that.adapter) : that.adapter == null;
  }

  @Override
  public int hashCode() {
    return adapter != null ? adapter.hashCode() : 0;
  }
}
