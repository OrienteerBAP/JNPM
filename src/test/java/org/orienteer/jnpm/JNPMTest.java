package org.orienteer.jnpm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.imageio.spi.RegisterableService;

import org.junit.BeforeClass;
import org.junit.Test;
import org.orienteer.jnpm.dm.RegistryInfo;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Unit test for simple App.
 */
public class JNPMTest 
{
	private static NPMRegistryService registerableService;
	
	@BeforeClass
	public static void init() {
		Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl("http://registry.npmjs.org/")
			    .addConverterFactory(JacksonConverterFactory.create())
			    .build();
		registerableService = retrofit.create(NPMRegistryService.class);
	}
    
    @Test
    public void registryInfoRetrival() throws IOException {
    	Call<RegistryInfo> info = registerableService.getRegistryInfo();
    	assertNotNull(info);
    	System.out.println(info.execute().body());
    }
}
