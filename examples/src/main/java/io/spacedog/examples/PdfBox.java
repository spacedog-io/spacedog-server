package io.spacedog.examples;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class PdfBox {

	public static void main(String[] args) throws Exception {
		String filename = "toto.pdf";
		String message = "Hello qdfzef zf a zfWorld";

		PDDocument doc = new PDDocument();
		PDPage page = new PDPage();
		doc.addPage(page);

		PDFont font = PDType1Font.HELVETICA_BOLD;

		PDPageContentStream contents = new PDPageContentStream(doc, page);
		contents.beginText();
		contents.setFont(font, 12);
		contents.setLeading(20);
		contents.newLineAtOffset(100, 700);
		contents.showText(message);
		contents.newLine();
		contents.showText(message);
		contents.endText();
		contents.close();

		doc.save(filename);
	}
}
