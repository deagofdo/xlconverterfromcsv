package com.xlconverter.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class Convert {
	List<String> questions =null;;
	String configConvert = null;
	JSONObject config = null;
	XSSFWorkbook workbook = null;
	XSSFSheet sheet = null;
	int rowCount = 0;

	public File importAndConvert(MultipartFile csv, MultipartFile configFile) throws Exception {
		File file = null;
		System.out.println("*** inside import and convert");
		String csvConvert = new String(csv.getBytes());
		configConvert = new String(configFile.getBytes());
		try (BufferedReader br = new BufferedReader(new StringReader(csvConvert))) {
			questions = Arrays.asList(br.readLine().split(","));
			Runnable task = () -> {
				System.out.println("*** inside runnable ");
				String line = null;
				List<String> answers = new ArrayList<>();
				
					try {
						while ((line = br.readLine()) != null) {
							answers = Arrays.asList(line.split(","));
							writeToSheet(answers);
						}
					} catch (Exception e) {
						System.out.println("*** something went wrong while reading the file-"+e.getMessage());
					}
				
			};
			Thread thread = new Thread(task);
			thread.run();
			file = new File(config.get("name").toString() + "_final.xlsx");
			System.out.println("*** writing to file "+ file.getName());
			try (FileOutputStream fw = new FileOutputStream(file)) {
				workbook.write(fw);
				
				workbook.close();
				workbook=null;
				rowCount=0;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IOException("*** error while writing file "+e.getMessage());
			}
		} catch (Exception e) {
throw new Exception(e.getMessage());
		}
		return file;

	}

	private synchronized void writeToSheet(List<String> answers) throws Exception {
		System.out.println("*** converting to excel");

		if (workbook == null) {
			int cellCount = 0;
			workbook = new XSSFWorkbook();
			try {
				config = (JSONObject) new JSONParser().parse(configConvert);
				sheet = workbook.createSheet(config.get("name") + "_sorted");
				JSONObject sections = (JSONObject) config.get("sections");
				if (!sections.isEmpty()) {
					Row row = sheet.createRow(rowCount++);
					int mergestart = 0;
					int mergeEnd = 0;
					for (Object o : sections.keySet()) {

						String heading = o.toString();
						//System.out.println("heading=" + heading);
						String[] numberOfQuestions = sections.get(heading).toString().split("-");
						int startNo = Integer.parseInt(numberOfQuestions[0]);
						int endNo = Integer.parseInt(numberOfQuestions[1]);
						if (startNo < endNo) {

							Cell cell = row.createCell(cellCount);
							cellCount = endNo;
							//System.out.println("cellcount==" + cellCount);
							cell.setCellValue(heading);
							mergeEnd = endNo - 1;
							sheet.addMergedRegion(new CellRangeAddress(0, 0, mergestart, mergeEnd));
							mergestart = endNo;
						} else {
							throw new IndexOutOfBoundsException("enter valid question sequence number");
						}

					}
				}
			} catch (ParseException e) {
				throw new Exception("Invalid JSON format");
			}

		}
		//writing questions
		if (rowCount == 1) {
			//System.out.println("writing questions");
			Row row = sheet.createRow(rowCount++);

			int flag = 0;
			int cellCount = 0;
			int questionSize = questions.size();
			for (int i = 1; i < questionSize; i++) {

				if (questions.get(i).toLowerCase().contains("wait time")
						|| questions.get(i).toLowerCase().contains("process time")) {
					flag++;

				}
				Cell cell = row.createCell(cellCount++);
				cell.setCellValue(questions.get(i).replaceAll("\"", ""));
				if (flag == 2) {
					Cell total = row.createCell(cellCount++);
					total.setCellValue("total time");
					flag = 0;
					continue;
				}
			}
		}
		// writing answers
		Row row = sheet.createRow(rowCount++);
		int cellCount = 0;
		int flag = 0;
		int questionSize = questions.size();
		CellStyle cs = null;
		for (int i = 1; i < questionSize; i++) {
			if (questions.get(i).toLowerCase().contains("wait time")
					|| questions.get(i).toLowerCase().contains("process time")) {
				flag++;
			}
			Cell cell = row.createCell(cellCount++);
			if (questions.get(i).toLowerCase().contains("wait time")
					|| questions.get(i).toLowerCase().contains("process time")) {
				cs = workbook.createCellStyle();
				cs.setDataFormat(workbook.createDataFormat().getFormat("[h]:mm:ss"));
				cell.setCellStyle(cs);
				cell.setCellValue(DateUtil.convertTime(answers.get(i).toString().replaceAll("\"", "")));
			} else {
				cell.setCellValue(answers.get(i).replaceAll("\"", ""));
			}
			if (flag == 2) {

				Cell total = row.createCell(cellCount++);
				total.setCellStyle(cs);
				total.setCellType(CellType.FORMULA);
				total.setCellFormula("SUM(" + row.getCell(cellCount - 3).getAddress() + ":"
						+ row.getCell(cellCount - 2).getAddress() + ")");
				flag = 0;
				continue;
			}
		}

	}
//scheduler to remove files-scheduled to delete after 2 days of file creation
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Calcutta")
	public void cronJob() throws IOException {
		clearFiles("expiry");
	}
	public void clearFiles(String from) throws IOException {
		BasicFileAttributes attrib = null;
		System.out.println("*** scheduler running");
		Path currentDirectory = Paths.get("");

		File file = new File(currentDirectory.toAbsolutePath().toString());
		File[] files = file.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				return name.endsWith(".xlsx");
			}
		});
		for (File f : files) {
			Path path = Paths.get(f.getAbsolutePath());
			try {
				attrib = Files.readAttributes(path, BasicFileAttributes.class);
				long createdTime = attrib.creationTime().toMillis();
				long now = System.currentTimeMillis();
				if(from.equals("expiry")) {
				if ((createdTime + (60 * 60 * 24 * 2)) < now) {
					f.delete();
				}}
				if(from.equals("shutdown")) {
					f.delete();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IOException("Error while reading file attributes"+e.getMessage());
			}

		}

	}
	
}
