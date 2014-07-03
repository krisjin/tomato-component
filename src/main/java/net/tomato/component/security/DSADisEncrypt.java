package net.tomato.component.security;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;

public class DSADisEncrypt {

	public static void main(String[] args) {
		try {
			// 其他用户用他的公共密钥(pubkey)和签名(signed)和信息(info)进行验证是否由他签名的信息
			// 读入公钥
			java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream("mypubkey.dat"));
			PublicKey pubkey = (PublicKey) in.readObject();
			in.close();

			// 读入签名和信息
			in = new java.io.ObjectInputStream(new java.io.FileInputStream("myinfo.dat"));
			String info = (String) in.readObject();
			byte[] signed = (byte[]) in.readObject();
			in.close();

			// 初始一个Signature对象,并用公钥和签名进行验证
			java.security.Signature signetcheck = java.security.Signature.getInstance("DSA");
			signetcheck.initVerify(pubkey);
			signetcheck.update(info.getBytes());
			if (signetcheck.verify(signed)) {
				System.out.println("签名正常");
			} else {
				System.out.println("签名不正常");
			}
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