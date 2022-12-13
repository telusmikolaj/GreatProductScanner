package pl.waw.great.greatproductscanner;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {

    @POST("barcode")
    Call<BarcodeScan> sendBarcode(@Body BarcodeScan barcodeScan);
}
