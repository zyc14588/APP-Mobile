package com.example.myapplication.Dao;

import android.content.ContentValues;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;

import com.example.myapplication.Activities.MainActivity;
import com.example.myapplication.Dao.Sql.AppSql;
import com.example.myapplication.DateStract.Tocken;
import com.example.myapplication.Utils.ByteUtils;
import com.example.myapplication.Utils.Util;
import com.example.myapplication.Utils.utils.sm4.SM4Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

/*
    作者：zyc14588
    github地址:https://github.com/zyc14588
*/public class Dao_Tocken {
    private volatile static Dao_Tocken sDao_tocken;
    private volatile static AppSql mDatabase;
    private Dao_Tocken(){}
    public static Dao_Tocken getInstance(AppSql sqLiteDatabase){
        if(sDao_tocken==null){
            synchronized (Dao_Tocken.class){
                if(sDao_tocken==null)
                    sDao_tocken=new Dao_Tocken();
            }

        }
        if(sqLiteDatabase==null) {
            throw new NotFoundException();
        }
        mDatabase=sqLiteDatabase;
        return sDao_tocken;
    }

    public void InsertTocken(Tocken tocken) throws Exception {
        String key= Util.byteToHex(SM4Utils.genersm4key());
        byte[] tocken_byte= ByteUtils.objectToByteArray(tocken);
        SM4Utils sm4=new SM4Utils();
        sm4.iv = "31313131313131313131313131313131";
        sm4.secretKey=key;
        sm4.hexString=true;
        byte[] Tocken_inc=sm4.encryptData_CBC(tocken_byte);
        String path= MainActivity.path;
        String name="/Tocken/"+new String(tocken.getUuid());
        File Tocken=new File(path,name);
        File dir=Tocken.getParentFile();
        if(dir!=null&&!dir.exists())
            dir.mkdirs();
        if(!Tocken.exists())
            try{
                Tocken.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        //else throw new FileAlreadyExistsException("Tocken has existed!");
        FileOutputStream fileOutputStream;
        try{
        fileOutputStream=new FileOutputStream(Tocken);
        fileOutputStream.write(Tocken_inc);
        fileOutputStream.flush();
        fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ContentValues values=new ContentValues();
        values.put("uuid", new String(tocken.getUuid()));
        values.put("dekey",key);
        mDatabase.insert("Tocken",values);
    }
    public Tocken SelectTocken(String uuid) throws IOException {
        String[]selarg=new String[]{uuid};
        String key = null;
        Cursor cursor=mDatabase.query("Tocken",new String[]{"dekey"},"uuid=?",selarg,null,null,null);
        boolean fist=cursor.moveToFirst();
        boolean isEmpty=cursor.getCount()==0;
        if(cursor!=null&&fist&&!isEmpty){
            key=cursor.getString(cursor.getColumnIndex("dekey"));
        }
        else throw new NoSuchElementException();
        cursor.close();
        SM4Utils sm4=new SM4Utils();
        sm4.iv = "31313131313131313131313131313131";
        sm4.secretKey=key;
        sm4.hexString=true;
        String name="/Tocken/"+uuid.substring(0,16);
        File Tocken_file=new File(MainActivity.path+name);
        if(!Tocken_file.exists())
            throw new FileNotFoundException();
        FileInputStream fileInputStream=new FileInputStream(Tocken_file);
        byte[] Tocken_inc=new byte[fileInputStream.available()];
        fileInputStream.read(Tocken_inc);
        byte[]Tocken_byte=sm4.decryptData_CBC(Tocken_inc);
        return (Tocken)ByteUtils.byteArrayToObject(Tocken_byte);
    }
    public void DeleteTocken(String uuid) throws IOException {
        mDatabase.delete("Tocken","uuid=?",new String[]{uuid});
        String name="/Tocken/"+uuid.substring(0,16);
        File Tocken_file=new File(MainActivity.path+name);
        if(!Tocken_file.exists())
            throw new FileNotFoundException();
        Tocken_file.delete();
    }
}
