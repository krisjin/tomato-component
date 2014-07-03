package net.tomato.component.security;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;

public class DSAEncrypt {

	public static void main(String[] args) {
		String myinfo = "test";
		try {
			java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream("myprikey.dat"));
			PrivateKey myprikey = (PrivateKey) in.readObject();
			in.close();
			// 初始一个Signature对象,并用私钥对信息签名
			java.security.Signature signet = java.security.Signature.getInstance("DSA");
			signet.initSign(myprikey);
			signet.update(myinfo.getBytes());
			byte[] signed = signet.sign();
			// 把信息和签名保存在一个文件中(myinfo.dat)
			java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(new java.io.FileOutputStream("myinfo.dat"));
			out.writeObject(myinfo);
			out.writeObject(signed);
			out.close();
		} catch (InvalidKeyException e) {

			e.printStackTrace();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {

			e.printStackTrace();
		} catch (SignatureException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		}
	}

}
