package com.xlconverter.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import javax.naming.directory.BasicAttributes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.xlconverter.converter.Convert;

@RestController
public class Controller {
	@Autowired
	Convert convert;
@PostMapping("/convert")
public ResponseEntity<String> convert(@RequestPart(required=true)MultipartFile csv,@RequestPart(required=true)MultipartFile config) {
	File file=null;
	try {
		if(csv.getOriginalFilename().contains(".csv")&&config.getOriginalFilename().contains(".json"))
		 {file=convert.importAndConvert(csv, config);
		return new ResponseEntity<String>("the file name is "+file.getName() ,HttpStatus.OK);
		 }else
			throw new IOException("Provice correct csv and json files");
		
	} catch (IOException e) {
		return new ResponseEntity<String>( e.getMessage(),HttpStatus.BAD_REQUEST);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		return new ResponseEntity<String>( e.getMessage(),HttpStatus.BAD_REQUEST);
	}
}
@GetMapping("/download/{filename}")
public ResponseEntity<Resource> download(@PathVariable("filename")String name){
	File file=new File(name+".xlsx");

	 Path path = Paths.get(file.getAbsolutePath());
	    ByteArrayResource resource;
		try {
			resource = new ByteArrayResource(Files.readAllBytes(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return new ResponseEntity<>(new ByteArrayResource("file not found".getBytes()),HttpStatus.NOT_FOUND);
		}
	    return ResponseEntity.ok().header("Content-Disposition", "attachment; filename="+name+".xlsx")
	            .contentType(MediaType.parseMediaType("application/octet-stream"))
	            .body(resource);
}

}
