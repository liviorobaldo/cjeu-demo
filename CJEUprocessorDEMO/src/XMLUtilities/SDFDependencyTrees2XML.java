package XMLUtilities;

import SDFTagger.SDFItems.SDFHead;
import SDFTagger.SDFItems.SDFDependencyTree;
import java.util.*;
import org.jdom2.*;

public class SDFDependencyTrees2XML 
{
        //This method return an ArrayList of Object which could be either org.jdom.Text or org.jdom.Element. 
        //The ArrayList(s) in the input taggedSDFHeads specify how to create the Element(s); the String[] associated with these ArrayList<SDFHead> are the XML tag(s)
        //on which tagging these SDFHead(s). If String[] is longer than 1, there are multiple tags to enclose in each other.
    
        //Of course, the SDFHead(s) ArrayList<SDFHead> must be all *contiguous*. And the ArrayList<SDFHead> must not intersect (however, they may be included into
        //one another). Otherwise they could lead to ill-formed XML tags. While we try to build the output ArrayList<Content>, we check this out and, 
        //if an ArrayList<SDFHead> is indeed ill-formed, we raise an exception.
    
        //Finally, it could be the case that more tags starts at the same SDFHead. For instance, we could have two tags "a" and "b" that start both at the word "sample"
        //for the sentence "This is a sample sentence", with the ArrayList<SDFHead> associated with "a" longer than the one associated with "b". 
        //In such example, of course we will have to build "This is a <a><b>sample</b> sentence</a>" and not "This is a <b><a>sample... AHHH!!! </b> closes before </a>!!!
        //On the other hand, there is no problem is the same SDFHead(s) are associated to multiple tags; in such a case, the String[] will have more than one element
        //and we have to enclose Element(s) from left to right according to the tags. 
    public static ArrayList<Content> SDFDependencyTrees2XML(ArrayList<SDFDependencyTree> SDFDependencyTrees, Hashtable<ArrayList<SDFHead>, String[]> taggedSDFHeads)throws Exception
    {
            //First of all, we define a practical Hashtable that associates the first SDFHead of each ArrayList with the ArrayList... or the ArrayList(s)! It could be more than one!
            //By the way, we create a copy of the ArrayList<SDFHead>, because we don't want to ruin the ones of the calling methods... 
        Hashtable<SDFHead, ArrayList<ArrayList<SDFHead>>> firstSDFHead2ArrayLists = new Hashtable<SDFHead, ArrayList<ArrayList<SDFHead>>>();
        Enumeration<ArrayList<SDFHead>> en = taggedSDFHeads.keys();
        while(en.hasMoreElements())
        {
            ArrayList<SDFHead> temp = en.nextElement();
            
            if(temp.isEmpty())throw new Exception("ill-formed tagging: one of the tags is not associated with any SDFHead!");
            if(taggedSDFHeads.get(temp).length==0)throw new Exception("ill-formed tagging: the SDFHead \""+temp.get(0).getForm()+"\" is not associated with any tag!");
            
            ArrayList<SDFHead> tempCOPY = new ArrayList<SDFHead>();
            for(SDFHead SDFHead:temp)tempCOPY.add(SDFHead);
            
            ArrayList<ArrayList<SDFHead>> arrayListsOfFirstSDFHead = firstSDFHead2ArrayLists.get(tempCOPY.get(0));
            if(arrayListsOfFirstSDFHead==null){arrayListsOfFirstSDFHead=new ArrayList<ArrayList<SDFHead>>();firstSDFHead2ArrayLists.put(tempCOPY.get(0), arrayListsOfFirstSDFHead);}
            arrayListsOfFirstSDFHead.add(tempCOPY);
        }
            
            //Then, we also need the ArrayList<SDFHead> of all SDFHead in the SDFDependencyTree(s) (in the same order!)
        ArrayList<SDFHead> allSDFHeads = new ArrayList<SDFHead>();
        for(SDFDependencyTree SDFDependencyTree:SDFDependencyTrees)
        {
                //We just add 1 blank between an SDFDependencyTree and the previous one, otherwise it will concatenate the two forms.
            SDFDependencyTree.getHeads()[0].setOptionalFeatures("blanksBefore", "1");
            for(int i=0;i<SDFDependencyTree.getHeads().length;i++)
                if(allSDFHeads.contains(SDFDependencyTree.getHeads()[i])==true)throw new Exception("ill-formed SDFDependencyTree: the same SDFHead occurs twice!");
                else allSDFHeads.add(SDFDependencyTree.getHeads()[i]);
        }
        
            //Now... we use the procedure SDFDependencyTrees2Element which is RECURSIVE.
            //We use an Element "root" because we could need to (RECURSIVELY!) create sub-Element(s) (and sub-sub-Element(s), etc.).
            //At the end, root.getContent() will give us our final output.
            //We will have an ArrayList<ArrayList<SDFHead>> SDFHeadsToConsume: at each step, the first element in *EACH* ArrayList<SDFHead> within SDFHeadsToConsume
            //must be THE SAME. This is the SDFHead we are consuming: we write its value in the Text.
            //At the beginning, SDFHeadsToConsume contains only allSDFHeads. Then, when we find firstSDFHead2ArrayList, we drop the Text in root, we create a
            //new Element with the tag, we add it also to root. Then, we take the ArrayList<SDFHead> and we add it on top of SDFHeadsToConsume. Then, we create a 
            //new Text that we add to the new Element and we go in recursion.
            //When the first ArrayList<SDFHead> on top of SDFHeadsToConsume is empty, we return from the recursion.
            
            //If there is an error somewhere, it means the input do not correspond to well-formed XML tagging. In such a case, we raise an exception.
        Element root = new Element("root");
        Text text = new Text("");
        root.getContent().add(text);
        ArrayList<ArrayList<SDFHead>> SDFHeadsToConsume = new ArrayList<ArrayList<SDFHead>>();
        SDFHeadsToConsume.add(allSDFHeads);
        SDFDependencyTrees2Element(root, text, SDFHeadsToConsume, taggedSDFHeads, firstSDFHead2ArrayLists);
        if(SDFHeadsToConsume.isEmpty()==false)throw new Exception("ill-formed tagged SDFHead(s)");//if SDFHeadsToConsume still contains SDFHead(s), there was something wrong.
        
        ArrayList<Content> ret = new ArrayList<Content>();
        for(Content c:root.getContent())ret.add(c.clone());
        return ret;
    }
    
    private static void SDFDependencyTrees2Element
    (
        Element root, 
        Text text, 
        ArrayList<ArrayList<SDFHead>> SDFHeadsToConsume,
        Hashtable<ArrayList<SDFHead>, String[]> taggedSDFHeads,
        Hashtable<SDFHead, ArrayList<ArrayList<SDFHead>>> firstSDFHead2ArrayLists
    )throws Exception
    {
            //Stop recursion.
        if(SDFHeadsToConsume.get(0).isEmpty()==true){SDFHeadsToConsume.remove(0);return;}
        
        ArrayList<SDFHead> firstArrayList = SDFHeadsToConsume.get(0);
        SDFHead firstSDFHeadOfFirstArrayList = firstArrayList.get(0);
        
            //Given an SDFHead, firstSDFHead2ArrayLists contain ALL ArrayList<SDFHead> that start on that SDFHead (if there is at least one).
            //We process now the longest one. For instance, if we have "a" and "b" that start both at the word "sample", with "a" longer than "b",
            //we will have to build "This is a <a><b>sample</b> sentence</a>" and not "This is a <b><a>sample... AHHH!!! </b> closes before </a>!
            
            //The problem is when <a> and <b> have the same lenght. In this case we could have either "This is a <a><b>sample sentence</b></a>"
            //or "This is a <b><a>sample sentence</a></b>". We choose the tag that come first alphabetically, i.e., <a> in this case.
            //If this is not the solution wanted by the calling procedure, *IT* will swap the two tags, not this method.
            //By the way, in this case the newArrayListToConsume is associated with a String[] having two String(s) by taggedSDFHeads.
            //So, we just create the Element(s) by processing the String[] alphabetically.
        ArrayList<ArrayList<SDFHead>> newArrayListsToConsume = firstSDFHead2ArrayLists.get(firstSDFHeadOfFirstArrayList);
        if(newArrayListsToConsume==null)
        {
                //if there is no newArrayListToConsume, there is no new Element to create. We remove firstSDFHeadOfFirstArrayList from EVERY ArrayList in SDFHeadsToConsume.
                //This SDFHead should be the first in each ArrayList. If it's not, the very input array corresponded to ill-formed XML tags => Exception!
                //If it is, we remove it from everyone and we add the form to the text. Then, we will go in recursion.
            consumeFirstSDFHead(text, SDFHeadsToConsume, firstSDFHeadOfFirstArrayList);
        }
        else
        {
                //Now we select the longer ArrayList<SDFHead> in newArrayListSToConsumes and we removed it from the ArrayList<ArrayList<SDFHead>>
                //(i.e., we also consume firstSDFHead2ArrayLists).
            ArrayList<SDFHead> newArrayListToConsume = newArrayListsToConsume.get(0);
            for(int i=1;i<newArrayListsToConsume.size();i++)
                if(newArrayListsToConsume.get(i).size()>newArrayListToConsume.size())
                    newArrayListToConsume=newArrayListsToConsume.get(i);
            
                //We remove the selected ArrayList<SDFHead> from newArrayListsToConsume (i.e., we consume firstSDFHead2ArrayLists),
                //otherwise we will loop infinitely!
            newArrayListsToConsume.remove(newArrayListToConsume);

                //If newArrayListsToConsume got empty, we remove it from firstSDFHead2ArrayLists. 
                //In some case, when the input ArrayList<SDFDependencyTree> SDFDependencyTrees is ill-formed, if I don't do this it raises an exception.
            if(newArrayListsToConsume.isEmpty()==true)firstSDFHead2ArrayLists.remove(firstSDFHeadOfFirstArrayList);
            
                //First, we deal with the previous text we were filling before finding this Element.
                //I mean: if we didn't add anything in text... we just remove it...
            if(text.getText().trim().compareToIgnoreCase("")==0)root.getContent().remove(text);
            
                //Now we create the new Element.. or possibly the new Element(s), if there is more than one (String[].length>1)
                //We do so by consuming orderedXmlTags (we won't need the ArrayList<String> anymore below, so who cares...)
                //NB. in case we have more Element(s) to create on the same newArrayListToConsume (i.e., we enter in the 
                //"while(orderedXmlTags.isEmpty()==false)", after the recursion we need to proceed from the outer root,
                //not the inner one! Thus, we save the current root in outerRoot and we'll restore it after the recursion.
            String[] xmlTags = taggedSDFHeads.get(newArrayListToConsume);
            if(xmlTags.length==0)throw new Exception("ill-formed tagged SDFHead(s)");
            Element newElement = new Element(xmlTags[0]);
            Element outerRoot = root;
            root.getContent().add(newElement);
            for(int i=1;i<xmlTags.length;i++)
            {
                root = newElement;
                newElement = new Element(xmlTags[i]);
                root.getContent().add(newElement);
            }    
            
                //Now we add a Text in newElement
            Text newText = new Text("");
            newElement.getContent().add(newText);
            
                //And newArrayListToConsume to SDFHeadsToConsume.
            SDFHeadsToConsume.add(0, newArrayListToConsume);
            
                //We consume an SDFHead (from all ArrayList<SDFHead> in SDFHeadsToConsume *BEFORE* going in recursion.
                //Otherwise in the next recursion it will pick the same newArrayListToConsume and we'll loop infinitely!
                //BUT!!! Only if newArrayListsToConsume is empty! Otherwise, we've to create at least another Element on this SDFHead! So we don't have to consume it.
            if(newArrayListsToConsume.isEmpty()==true)consumeFirstSDFHead(newText, SDFHeadsToConsume, firstSDFHeadOfFirstArrayList);
            
                //And finally... recursion! On the newElement and the newText, of course, not on root and text. 
                //The recursion on on root and text will be done just after (see last instruction of the method).
            SDFDependencyTrees2Element(newElement, newText, SDFHeadsToConsume, taggedSDFHeads, firstSDFHead2ArrayLists);
            
                //After we have created and added a new Element... we need a new Text 
                //in order to write the text *AFTER* that Element in the next recursion (last instruction of the method, just three lines below).
            root = outerRoot;//we restore the outer root, in case we've just created more Element(s) on the same ArrayList<SDFHead>.
            text = new Text("");
            root.getContent().add(text);
        }
        
            //Either if we updated the text or we recursively created a new Element, we go in recursion until SDFHeadsToConsume is empty.
        SDFDependencyTrees2Element(root, text, SDFHeadsToConsume, taggedSDFHeads, firstSDFHead2ArrayLists);
    }
    
    private static void SDFDependencyTrees2ElementOLD
    (
        Element root, 
        Text text, 
        ArrayList<ArrayList<SDFHead>> SDFHeadsToConsume,
        Hashtable<ArrayList<SDFHead>, String[]> taggedSDFHeads,
        Hashtable<SDFHead, ArrayList<ArrayList<SDFHead>>> firstSDFHead2ArrayLists
    )throws Exception
    {
            //Stop recursion.
        if(SDFHeadsToConsume.get(0).isEmpty()==true){SDFHeadsToConsume.remove(0);return;}
        
        ArrayList<SDFHead> firstArrayList = SDFHeadsToConsume.get(0);
        SDFHead firstSDFHeadOfFirstArrayList = firstArrayList.get(0);
        
            //Given an SDFHead, firstSDFHead2ArrayLists contain ALL ArrayList<SDFHead> that start on that SDFHead (if there is at least one).
            //We process now the longest one. For instance, if we have "a" and "b" that start both at the word "sample", with "a" longer than "b",
            //we will have to build "This is a <a><b>sample</b> sentence</a>" and not "This is a <b><a>sample... AHHH!!! </b> closes before </a>!
            
            //The problem is when <a> and <b> have the same lenght. In this case we could have either "This is a <a><b>sample sentence</b></a>"
            //or "This is a <b><a>sample sentence</a></b>". We choose the tag that come first alphabetically, i.e., <a> in this case.
            //If this is not the solution wanted by the calling procedure, *IT* will swap the two tags, not this method.
            //By the way, in this case the newArrayListToConsume is associated with a String[] having two String(s) by taggedSDFHeads.
            //So, we just create the Element(s) by processing the String[] alphabetically.
        ArrayList<ArrayList<SDFHead>> newArrayListsToConsume = firstSDFHead2ArrayLists.get(firstSDFHeadOfFirstArrayList);
        if(newArrayListsToConsume==null)
        {
                //if there is no newArrayListToConsume, there is no new Element to create. We remove firstSDFHeadOfFirstArrayList from EVERY ArrayList in SDFHeadsToConsume.
                //This SDFHead should be the first in each ArrayList. If it's not, the very input array corresponded to ill-formed XML tags => Exception!
                //If it is, we remove it from everyone and we add the form to the text. Then, we will go in recursion.
            consumeFirstSDFHead(text, SDFHeadsToConsume, firstSDFHeadOfFirstArrayList);
        }
        else
        {
                //Now we select the longer ArrayList<SDFHead> in newArrayListSToConsumes and we removed it from the ArrayList<ArrayList<SDFHead>>
                //(i.e., we also consume firstSDFHead2ArrayLists).
            ArrayList<SDFHead> newArrayListToConsume = newArrayListsToConsume.get(0);
            for(int i=1;i<newArrayListsToConsume.size();i++)
                if(newArrayListsToConsume.get(i).size()>newArrayListToConsume.size())
                    newArrayListToConsume=newArrayListsToConsume.get(i);
            
                //We remove the selected ArrayList<SDFHead> from newArrayListsToConsume (i.e., we consume firstSDFHead2ArrayLists),
                //otherwise we will loop infinitely!
            newArrayListsToConsume.remove(newArrayListToConsume);
        
                //Now, we can have one or more XML tags associated with this ArrayList. We create them by nesting them from the one who
                //come first alphabetically to the last one who come from alphabetically. We use an additional ArrayList<String> for that
                //and the selection sort on the String[] elements.
            String[] xmlTags = taggedSDFHeads.get(newArrayListToConsume);
            ArrayList<String> orderedXmlTags = new ArrayList<String>();
            while(true)
            {
                int lowestI=-1;
                for(int i=0;i<xmlTags.length;i++)
                    if(xmlTags[i]!=null)
                        if((lowestI==-1)||(xmlTags[lowestI].compareToIgnoreCase(xmlTags[i])>0))
                            lowestI = i;
                if(lowestI==-1)break;
                
                orderedXmlTags.add(xmlTags[lowestI]);
                xmlTags[lowestI]=null;
            }
            
                //First, we deal with the previous text we were filling before finding this Element.
                //I mean: if we didn't add anything in text... we just remove it...
            if(text.getText().trim().compareToIgnoreCase("")==0)root.getContent().remove(text);
            
                //Now we create the new Element.. or possibly the new Element(s), if there is more than one. 
                //We do so by consuming orderedXmlTags (we won't need the ArrayList<String> anymore below, so who cares...)
                //NB. in case we have more Element(s) to create on the same newArrayListToConsume (i.e., we enter in the 
                //"while(orderedXmlTags.isEmpty()==false)", after the recursion we need to proceed from the outer root,
                //not the inner one! Thus, we save the current root in outerRoot and we'll restore it after the recursion.
            Element newElement = new Element(orderedXmlTags.remove(0));
            Element outerRoot = root;
            root.getContent().add(newElement);
            while(orderedXmlTags.isEmpty()==false)
            {
                root = newElement;
                newElement = new Element(orderedXmlTags.remove(0));
                root.getContent().add(newElement);
            }    
            
                //Now we add a Text in newElement
            Text newText = new Text("");
            newElement.getContent().add(newText);
            
                //And newArrayListToConsume to SDFHeadsToConsume.
            SDFHeadsToConsume.add(0, newArrayListToConsume);
            
                //We consume an SDFHead (from all ArrayList<SDFHead> in SDFHeadsToConsume *BEFORE* going in recursion.
                //Otherwise in the next recursion it will pick the same newArrayListToConsume and we'll loop infinitely!
                //BUT!!! Only if newArrayListsToConsume is empty! Otherwise, we've to create at least another Element on this SDFHead! So we don't have to consume it.
            if(newArrayListsToConsume.isEmpty()==true)consumeFirstSDFHead(newText, SDFHeadsToConsume, firstSDFHeadOfFirstArrayList);
            
                //And finally... recursion! On the newElement and the newText, of course, not on root and text. 
                //The recursion on on root and text will be done just after (see last instruction of the method).
            SDFDependencyTrees2Element(newElement, newText, SDFHeadsToConsume, taggedSDFHeads, firstSDFHead2ArrayLists);
            
                //After we have created and added a new Element... we need a new Text 
                //in order to write the text *AFTER* that Element in the next recursion (last instruction of the method, just three lines below).
            root = outerRoot;//we restore the outer root, in case we've just created more Element(s) on the same ArrayList<SDFHead>.
            text = new Text("");
            root.getContent().add(text);
        }
        
            //Either if we updated the text or we recursively created a new Element, we go in recursion until SDFHeadsToConsume is empty.
        SDFDependencyTrees2Element(root, text, SDFHeadsToConsume, taggedSDFHeads, firstSDFHead2ArrayLists);
    }
    
    private static void consumeFirstSDFHead(Text text, ArrayList<ArrayList<SDFHead>> SDFHeadsToConsume, SDFHead SDFHeadToConsume)throws Exception
    {
        for(ArrayList<SDFHead> temp:SDFHeadsToConsume)
        {
            if(temp.get(0)!=SDFHeadToConsume)throw new Exception("ill-formed tagged SDFHead(s)");
            temp.remove(0);
        }

        text.setText(updateText(text.getText(), SDFHeadToConsume));
    }
            
        //This method update the text by adding the new SDFHead. In case the SDFHead has an optional feature "blanksBefore", corresponding blanks are added.
        //Default blanksBefore is 1.
    private static String updateText(String text, SDFHead SDFHead)
    {
        text=text.trim();
        
        int blanksBefore = 1;
        
        String blanksBeforeString = SDFHead.getOptionalFeaturesValue("blanksBefore");
        
        if(blanksBeforeString!=null)blanksBefore = Integer.parseInt(blanksBeforeString);
        
        for(int i=0;i<blanksBefore;i++)text=text+" ";
        
        return (text+SDFHead.getForm().trim()).trim();
    }    
}