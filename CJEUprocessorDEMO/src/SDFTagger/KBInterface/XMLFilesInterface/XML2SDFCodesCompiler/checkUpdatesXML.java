package SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler;

import java.io.*;
import java.util.*;

/* 
    This class checks whether one of the XML files has been modified. If so, it recompiles all sources. We need to recompile them all in that we need to assign unique
    IDs to the SDFRule(s). In other words: we cannot recompile only the file that has been changed as that file could have some IDs that appear in other files; the 
    safest way is to recompile everything. We would need to implement more advanced code to recompile only the files that are changed while, at the same time, 
    guaranteeing that IDs are uniques in all files. This is left for future work (maybe).

    This class is recalled everytime XMLFilesManager is instantiated.
*/

public class checkUpdatesXML
{
    public static void updateCompiledKB(File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules)throws Exception 
    {
        if(detectChanges(rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules)==false)return;
        compileKB.compile(rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
    }
    
        //This procedure checks if the folder of XML files and the one of compiled files have misalignments. If there is even a single change, it returns true.
        //We take localPathsSDFRules and we divide between files and subfolder. 
        //- We check if at least one file is missing from rootDirectoryCompiledSDFRules or if one of them is changed. If so, we return true.
        //- We create a new String[] to recur on the subfolder. Note that the new rootDirectoryCompiledSDFRules could not exist! If there is a single XML
        //  file, we will return true.
    private static boolean detectChanges(File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules)throws Exception 
    {
        Hashtable<String, ArrayList<String>> subfolderFiles = new Hashtable<String, ArrayList<String>>();
        
        for(int i=0;i<localPathsSDFRules.length;i++)
        {
                //if this is true, we check the file
            if((localPathsSDFRules[i].indexOf("/")==-1)&&(localPathsSDFRules[i].indexOf("\\")==-1))
            {
                File XMLFile = new File(rootDirectoryXmlSDFRules.getAbsolutePath()+"/"+localPathsSDFRules[i]);
                File compiledSDFFile = new File(rootDirectoryCompiledSDFRules.getAbsolutePath()+"/"+localPathsSDFRules[i].substring(0, localPathsSDFRules[i].length()-4)+".sdf");
                
                    //if the XML does not exist, we kill the process...
                if(XMLFile.exists()==false){System.out.println("The file "+XMLFile.getAbsolutePath()+" does not exist!");System.exit(0);}
                if(compiledSDFFile.exists()==false)return true;//if the compiled one does not exist, we must recompile everything!
                
                    //If they both exist, but the size is different (the first line of the compiled file contains the latest size of the XML file),
                    //we need to recompile everythin
                InputStream is = new FileInputStream(compiledSDFFile);
                BufferedReader bf = new BufferedReader(new InputStreamReader(is, "UTF8"));
                long oldsize = Long.parseLong(bf.readLine());
                bf.close();
                is.close();
                if(oldsize!=XMLFile.length())return true;
                
                continue;
            }
            else
            {
                String localPath = localPathsSDFRules[i];
                String subFolder = "";
                
                if((localPath.indexOf("/")==-1)||((localPath.indexOf("\\")!=-1)&&(localPath.indexOf("\\")<localPath.indexOf("/"))))
                {
                    subFolder = localPath.substring(0, localPath.indexOf("\\"));
                    localPath  = localPath.substring(localPath.indexOf("\\")+1, localPath.length());
                }
                else if((localPath.indexOf("\\")==-1)||((localPath.indexOf("/")!=-1)&&(localPath.indexOf("/")<localPath.indexOf("\\"))))
                {
                    subFolder = localPath.substring(0, localPath.indexOf("/"));
                    localPath  = localPath.substring(localPath.indexOf("/")+1, localPath.length());
                }
                
                ArrayList<String> temp = subfolderFiles.get(subFolder);
                if(temp==null)
                {
                    temp = new ArrayList<String>();
                    subfolderFiles.put(subFolder, temp);
                }
                
                temp.add(localPath);
            }
        }
        
        Enumeration en = subfolderFiles.keys();
        while(en.hasMoreElements())
        {
            String subFolder = (String)en.nextElement();
            ArrayList<String> subPaths = subfolderFiles.get(subFolder);
            String[] subPathsSDFRules = new String[subPaths.size()];
            for(int i=0;i<subPaths.size();i++)subPathsSDFRules[i]=subPaths.get(i);
            
            if(detectChanges(
                new File(rootDirectoryXmlSDFRules.getAbsolutePath()+"/"+subFolder), 
                new File(rootDirectoryCompiledSDFRules.getAbsolutePath()+"/"+subFolder), 
                subPathsSDFRules)==true)return true;
        }
        
        return false;
    }
}

