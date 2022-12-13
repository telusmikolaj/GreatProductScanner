package pl.waw.great.greatproductscanner;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClinet {
    public static Retrofit retroFit;

    public static Retrofit getRetroFitInstance() {
        if (retroFit == null) {
            return retroFit = new Retrofit.Builder()
                    .baseUrl("http://192.168.1.31:8082/product/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
            return retroFit;

        }
    }

