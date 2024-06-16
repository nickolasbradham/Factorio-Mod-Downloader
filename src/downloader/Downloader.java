package downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;

final class Downloader {

	private final String raw, user, pass;
	private int parsed = 0;

	private Downloader(String username, String password) throws MalformedURLException, IOException, URISyntaxException {
		raw = new String(new URI("https://mods.factorio.com/api/mods?version=1.1&page_size=max").toURL().openStream()
				.readAllBytes());
		user = username;
		pass = password;
	}

	private synchronized String[] next() {
		parsed = raw.indexOf("\"download_url\": \"", parsed) + 17;

		if (parsed >= 17)
			return new String[] { raw.substring(parsed, parsed = raw.indexOf('"', parsed + 1)), raw.substring(
					parsed = raw.indexOf("\"file_name\": \"", parsed) + 14, parsed = raw.indexOf('"', parsed + 1)) };

		parsed = raw.length();
		return null;
	}

	private void start() throws IOException, InterruptedException, URISyntaxException {
		File saveLoc = new File("downloads");
		if (!saveLoc.exists())
			saveLoc.mkdir();

		HttpURLConnection http = (HttpURLConnection) new URI("https://auth.factorio.com/api-login").toURL()
				.openConnection();
		http.setRequestMethod("POST");
		http.setDoOutput(true);
		http.connect();
		http.getOutputStream().write(("password=" + pass + "&username=" + user).getBytes());

		if (http.getResponseCode() != 200) {
			System.out.println("Error trying to login. Check your username and password.");
			return;
		}

		String tok = new String(http.getInputStream().readAllBytes()).replaceAll("\\[|\\\"|\\]", "");
		Thread[] threads = new Thread[4];
		for (byte i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				String[] s;
				while ((s = next()) != null)
					try {
						FileOutputStream fos = new FileOutputStream(new File(saveLoc, s[1]));
						System.out.println("Downloading: " + s[1]);
						fos.getChannel().transferFrom(Channels.newChannel(
								new URI("https://mods.factorio.com" + s[0] + "?username=" + user + "&token=" + tok)
										.toURL().openStream()),
								0, Long.MAX_VALUE);
						fos.close();
					} catch (IOException | URISyntaxException e) {
						e.printStackTrace();
					}
			});

			threads[i].start();
		}

		for (Thread t : threads)
			t.join();

		System.out.println("Done.");
	}

	public static void main(String[] args)
			throws MalformedURLException, IOException, InterruptedException, URISyntaxException {
		if (args.length != 2) {
			System.out.println(
					"Args: <username> <password>\n  username: Your Factorio mod portal username.\n  password: Your password.");
			return;
		}
		new Downloader(args[0], args[1]).start();
	}
}