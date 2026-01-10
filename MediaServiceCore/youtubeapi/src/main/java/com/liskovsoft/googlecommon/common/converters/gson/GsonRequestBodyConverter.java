 
package com.liskovsoft.googlecommon.common.converters.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

final class GsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
  private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");
  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private final Gson gson;
  private final TypeAdapter<T> adapter;

  GsonRequestBodyConverter(Gson gson, TypeAdapter<T> adapter) {
    this.gson = gson;
    this.adapter = adapter;
  }

  @Override public RequestBody convert(T value) throws IOException {
     
     
     
     
     
     
     

     
     

     
    return RequestBody.create(MEDIA_TYPE, value instanceof String ? (String) value : gson.toJson(value));
  }
}
