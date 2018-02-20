package com.xlconverter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.xlconverter.converter.Convert;
@SpringBootApplication
@EnableScheduling
public class XlConverterApplication {
	public static void main(String[] args) {
		SpringApplication.run(XlConverterApplication.class, args);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				Convert convert = new Convert();
		    	try {
		    		System.out.println("inside shutdown");
					convert.clearFiles("shutdown");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}) );
}
}