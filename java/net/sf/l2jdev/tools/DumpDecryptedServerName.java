package net.sf.l2jdev.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.sf.l2jdev.actions.OpenDat;
import net.sf.l2jdev.clientcryptor.DatFile;
import net.sf.l2jdev.clientcryptor.crypt.DatCrypter;
import net.sf.l2jdev.config.ConfigDebug;
import net.sf.l2jdev.config.ConfigWindow;
import net.sf.l2jdev.xml.CryptVersionParser;

public class DumpDecryptedServerName
{
	public static void main(String[] args)
	{
		
		ConfigDebug.load();
		CryptVersionParser.getInstance().parse();
		
		String inputPath = null;
		if ((args != null) && (args.length > 0) && (args[0] != null) && !args[0].trim().isEmpty())
		{
			inputPath = args[0];
		}
		else
		{
			inputPath = ConfigWindow.LAST_FILE_SELECTED;
		}
		
		final File inFile = new File(inputPath);
		if (!inFile.exists())
		{
			System.err.println("not found: " + inFile.getAbsolutePath());
			return;
		}
		
		System.out.println("encrypted file: " + inFile.getAbsolutePath());
		
		final String encryptSelection = ConfigWindow.CURRENT_ENCRYPT;
		DatCrypter crypter = null;
		if ((encryptSelection == null) || encryptSelection.equalsIgnoreCase(".") || encryptSelection.equalsIgnoreCase("Source") || encryptSelection.trim().isEmpty())
		{
			// Use last decryptor detected by OpenDat.
			final DatCrypter lastDatDecryptor = OpenDat.getLastDatCrypter(inFile);
			if (lastDatDecryptor != null)
			{
				crypter = CryptVersionParser.getInstance().getDecryptKey(lastDatDecryptor.getName());
			}
		}
		else
		{
			crypter = CryptVersionParser.getInstance().getDecryptKey(encryptSelection);
		}
		
		if (crypter == null)
		{
			System.err.println("Decrypt key not found. CURRENT_ENCRYPT='" + encryptSelection + "', lastDatDecryptor=" + OpenDat.getLastDatCrypter(inFile));
			System.err.println("Available decrypt keys: " + CryptVersionParser.getInstance().getDecryptKeys().keySet());
			return;
		}
		
		// Create DatFile wrapper and decrypt.
		final DatFile datFile = new DatFile(inFile.getAbsolutePath());
		try
		{
			datFile.decrypt(crypter);
		}
		catch (Exception e)
		{
			System.err.println("Failed to decrypt file: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		
		final ByteBuffer buff = datFile.getBuff();
		if (buff == null)
		{
			System.err.println("Decryption returned null buffer.");
			return;
		}
		
		final byte[] decrypted = new byte[buff.remaining()];
		buff.get(decrypted);
		
		// Build output path based on input file name (foo.dat -> foo.dat.dec).
		final File outFile = new File(inFile.getName() + ".dec");
		try (FileOutputStream fos = new FileOutputStream(outFile))
		{
			fos.write(decrypted);
		}
		catch (IOException e)
		{
			System.err.println("Failed to write output file: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		
		System.out.println("Decrypted bytes written to: " + outFile.getAbsolutePath());
	}
}