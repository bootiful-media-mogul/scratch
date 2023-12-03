package com.joshlong.mogul.api.podcasts.archives;

import com.joshlong.mogul.api.podcasts.PodcastArchive;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Any client will upload a {@literal .zip} file. This program knows how to read and write
 * them, and the manifest inside them. This archive is the input into the podcast
 * publication pipeline.
 */
class ArchiveConverter implements Serializer<PodcastArchive>, Deserializer<PodcastArchive> {

	private final File extractionRoot;

	ArchiveConverter(File extractionRoot) {
		this.extractionRoot = extractionRoot;
	}

	private static void addFileToZip(InputStream is, String fileName, ZipOutputStream os) throws IOException {
		var zipEntry = new ZipEntry(fileName);
		os.putNextEntry(zipEntry);
		var next = -1;
		while ((next = is.read()) != -1)
			os.write(next);
		os.closeEntry();
	}

	private String encodeXmlDocument(String uuid, String title, String description, String intro, String interview,
									 String image) throws ParserConfigurationException, TransformerException, IOException {

		var dbFactory = DocumentBuilderFactory.newInstance();

		var dBuilder = dbFactory.newDocumentBuilder();
		var doc = dBuilder.newDocument();

		// root element
		var rootElement = doc.createElement("podcast");
		rootElement.setAttribute("title", title);
		rootElement.setAttribute("uid", uuid);
		doc.appendChild(rootElement);

		// interview element
		var interviewElem = doc.createElement("interview");
		interviewElem.setAttribute("src", interview);
		rootElement.appendChild(interviewElem);

		// introduction element
		var introElem = doc.createElement("introduction");
		introElem.setAttribute("src", intro);
		rootElement.appendChild(introElem);

		// photo element
		var photoElem = doc.createElement("photo");
		photoElem.setAttribute("src", image);
		rootElement.appendChild(photoElem);

		// description element with CDATA
		var descElem = doc.createElement("description");
		var cdata = doc.createCDATASection(description);
		descElem.appendChild(cdata);
		rootElement.appendChild(descElem);

		// write content to XML file
		var transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

		// dom source
		var source = new DOMSource(doc);
		try (var sw = new StringWriter()) {
			var result = new StreamResult(sw);
			transformer.transform(source, result);
			sw.flush();
			return sw.toString();
		}
	}

	@Override
	public void serialize(@NonNull PodcastArchive archive, @NonNull OutputStream outputStream) throws IOException {
		try (var zos = new ZipOutputStream(outputStream)) {

			for (var resource : new Resource[]{archive.introduction(), archive.interview(), archive.image()}) {
				try (var input = resource.getInputStream()) {
					addFileToZip(input, resource.getFilename(), zos);
				}
			}

			var xml = encodeXmlDocument(archive.uuid(), archive.title(), archive.description(),
					archive.introduction().getFilename(), archive.interview().getFilename(),
					archive.image().getFilename());
			try (var byteArrayInputStream = new BufferedInputStream(new ByteArrayInputStream(xml.getBytes()))) {
				addFileToZip(byteArrayInputStream, "manifest.xml", zos);
			}
		} //
		catch (Throwable throwable) {
			throw new IllegalArgumentException("there's been an exception encoding the archive", throwable);
		}
	}

	private PodcastArchive readPodcastArchiveFor(File extractionRoot, String xml) throws Exception {
		try (var inputStream = new ByteArrayInputStream(xml.getBytes())) {
			var factory = DocumentBuilderFactory.newInstance();
			var builder = factory.newDocumentBuilder();
			var doc = builder.parse(inputStream);
			var root = doc.getDocumentElement();
			var uuid = root.getAttribute("uid");
			var title = root.getAttribute("title");
			var introNodes = doc.getElementsByTagName("introduction");
			var intro = introNodes.item(0).getAttributes().getNamedItem("src").getNodeValue();
			var interviewNodes = doc.getElementsByTagName("interview");
			var interview = interviewNodes.item(0).getAttributes().getNamedItem("src").getNodeValue();
			var imageNodes = doc.getElementsByTagName("photo");
			var image = imageNodes.item(0).getAttributes().getNamedItem("src").getNodeValue();
			var descriptionNodes = doc.getElementsByTagName("description");
			var description = descriptionNodes.item(0).getTextContent();
			var podcastArchive = new PodcastArchive(uuid, title, description,
					new FileSystemResource(new File(extractionRoot, intro)),
					new FileSystemResource(new File(extractionRoot, interview)),
					new FileSystemResource(new File(extractionRoot, image)));
			Map.of("image", podcastArchive.image(), "intro", podcastArchive.introduction(), "interview",
							podcastArchive.introduction())
					.forEach((fn, r) -> Assert.state(r.exists(), "file for " + fn + " does not exist"));
			return podcastArchive;
		}
	}

	@Override
	public PodcastArchive deserialize(InputStream inputStream) throws IOException {
		var buffer = new byte[1024];
		var tmp = UUID.randomUUID().toString();
		var tmpFile = new File(this.extractionRoot, tmp);
		Assert.state(tmpFile.exists() || tmpFile.mkdirs(), "the directory must exist");
		try (var zis = new ZipInputStream(inputStream)) {
			var zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				var fileName = zipEntry.getName();
				try (var fos = new FileOutputStream(new File(tmpFile, fileName))) {
					var len = -1;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
				}
				zis.closeEntry();
				zipEntry = zis.getNextEntry();
			}
		}
		Assert.state(tmpFile.exists() && Objects.requireNonNull(tmpFile.list()).length >= 4,
				"there should now be at least four files");
		var manifestFile = new File(tmpFile, "manifest.xml");
		Assert.state(manifestFile.exists(), "the manifest XML does not exist. this is an invalid archive");
		try (var manifestXmlFile = new InputStreamReader(new FileInputStream(manifestFile))) {
			return readPodcastArchiveFor(tmpFile, FileCopyUtils.copyToString(manifestXmlFile));
		} //
		catch (Throwable t) {
			throw new RuntimeException("there's been an error reading the archive", t);
		}
	}

}
