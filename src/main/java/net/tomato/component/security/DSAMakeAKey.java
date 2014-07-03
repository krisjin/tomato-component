package net.tomato.component.security;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * @author krisjin
 */
public class DSAMakeAKey {

	public static void main(String[] args) {
		try {
			java.security.KeyPairGenerator keygen = java.security.KeyPairGenerator.getInstance("DSA");
			// 如果设定随机产生器就用如相代码初始化
			SecureRandom secrand = new SecureRandom();
			secrand.setSeed("tttt".getBytes()); // 初始化随机产生器
			keygen.initialize(512, secrand); // 初始化密钥生成器
			// 否则
			keygen.initialize(512);
			// 生成密钥公钥pubkey和私钥prikey
			KeyPair keys = keygen.generateKeyPair(); // 生成密钥组
			PublicKey pubkey = keys.getPublic();
			PrivateKey prikey = keys.getPrivate();
			// 分别保存在myprikey.dat和mypubkey.dat中,以便下次不在生成
			// (生成密钥对的时间比较长
			java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(new java.io.FileOutputStream("myprikey.dat"));
			out.writeObject(prikey);
			out.close();
			out = new java.io.ObjectOutputStream(new java.io.FileOutputStream("mypubkey.dat"));
			out.writeObject(pubkey);
			out.close();
		} catch (NoSuchAlgorithmException e) {
			// TODO 自动生成 catch 块
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO 自动生成 catch 块
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自动生成 catch 块
			e.printStackTrace();
		}

	}

}
