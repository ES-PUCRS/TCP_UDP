package libs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;

import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;

public class FileManager {

	public static byte[] read(String fileName) throws IOException {
		return Files.readAllBytes(Paths.get("./files/" + fileName));
	}

	public static void write(String fileName, byte[] file) throws IOException {
		Files.write(Paths.get("./files/received/"+fileName), file);
	}

	public static Map<Integer, String> split(byte[] file) throws IOException {
		Map<Integer, String> book = new HashMap<Integer, String>();
		int bookLength = 200;
		int dataLength = (file.length / bookLength) + 1;

		for(int from, to=0, i=0; i < dataLength; i ++){
			from = to; to += bookLength;
			if(to > file.length) to = file.length;
			book.put(
				i,
				Arrays.toString(
					Arrays.copyOfRange(
						file, from, to
					)
				)
			);
		}

		return book;
	}

}