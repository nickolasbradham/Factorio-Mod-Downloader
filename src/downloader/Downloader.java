package downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;

final class Downloader {

	private final String raw;
	private int parsed = 0;

	private Downloader() throws MalformedURLException, IOException {
		raw = new String(
				new URL("https://mods.factorio.com/api/mods?version=1.1&page_size=max").openStream().readAllBytes());
	}

	private synchronized String next() {
		parsed = raw.indexOf("\"download_url\": \"", parsed) + 17;

		if (parsed >= 17)
			return raw.substring(parsed, parsed = raw.indexOf('"', parsed + 1));

		parsed = raw.length();
		return null;
	}

	private void start() throws IOException, InterruptedException {
		File saveLoc = new File("downloads");
		if (!saveLoc.exists())
			saveLoc.mkdir();

		Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors()];
		for (byte i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				String s;
				while ((s = next()) != null)
					try {
						FileOutputStream fos = new FileOutputStream(new File(saveLoc, parsed + ".zip"));
						System.out.println("Downloading: " + s.substring(s.indexOf('/', 1) + 1, s.lastIndexOf('/')));
						fos.getChannel()
								.transferFrom(
										Channels.newChannel(new URL("https://mods.factorio.com" + s + "?username="
												+ Login.USERNAME + "&token=" + Login.TOKEN).openStream()),
										0, Long.MAX_VALUE);
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			});

			threads[i].start();
		}

		for (Thread t : threads)
			t.join();

		System.out.println("Done.");
	}

	public static void main(String[] args) throws MalformedURLException, IOException, InterruptedException {
		new Downloader().start();
	}
}