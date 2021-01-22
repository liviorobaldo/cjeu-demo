package SDFTagger;

import SDFTagger.SDFItems.SDFDependencyTree;
import java.util.*;
import SDFTagger.SDFItems.*;


    //This class has a single public method: "tagTrees", which takes a set of SDFDependencyTree(s) and returns a list of tags on the SDFHead(s) in the trees.
    //All other methods are private.
public class SDFTagger
{    
    protected SDFTaggerConfig SDFTaggerConfig = null;
    public SDFTagger(SDFTaggerConfig SDFTaggerConfig)throws Exception{this.SDFTaggerConfig=SDFTaggerConfig;}
    public SDFTaggerConfig getSDFTaggerConfig(){return SDFTaggerConfig;}
    
    public ArrayList<SDFTag> tagTrees(ArrayList<SDFDependencyTree> trees) throws Exception
    {
            //firstSDFNode is a fake SDFNode; after executing buildSDFNodesChain:
            //- firstSDFNode.nextNode points to the FIRST node of the bidirectional chain
            //- firstSDFNode.prevNode points to the LAST node of the bidirectional chain
        SDFNode firstSDFNode = new SDFNode(null, 0, null, null, false);
        buildSDFNodesChain(trees, firstSDFNode);
        
            //idInstance ensures that each *execution* of each SDFRule is assigned a unique ID.
        long idInstance = 1;
        ArrayList<SDFTag> ret = new ArrayList<SDFTag>();
        SDFNode scanSDFNode = firstSDFNode.nextSDFNode;
        while(scanSDFNode!=null)
        {
            //System.out.println(scanSDFNode.SDFHead.getForm());
            if
            (
                (scanSDFNode.SDFHead.getForm().compareToIgnoreCase("Admin")==0)
                //&&(scanSDFNode.nextSDFNode!=null)&&(scanSDFNode.nextSDFNode.SDFHead!=null)&&(scanSDFNode.nextSDFNode.SDFHead.getForm().compareToIgnoreCase(")")==0)
            )
                scanSDFNode=scanSDFNode;
            
            ArrayList<String> SDFCodes = SDFTaggerConfig.KBManager.retrieveSDFCodes(scanSDFNode.SDFHead);
            for(int i=0;i<SDFCodes.size();i++)
            {
                SDFRule SDFRule = new SDFRule(SDFCodes.get(i), SDFTaggerConfig.SDFNodeConstraintsFactory, this);
                
                if(SDFRule.id==2)
                    i=i;
                
                ArrayList<SDFTag> newTags = SDFRule.executeSDFRule(scanSDFNode, idInstance++);
                addTags(ret, newTags);
            }   
            
            scanSDFNode = scanSDFNode.nextSDFNode;
        }
        
        
        //piccolo controllo (da tenere per un bel po'). Voglio proprio vedere se le priorità sono in ordine
        for(int i=0;i<ret.size()-2;i++)
        {
            if(!(ret.get(i).priority>=ret.get(i+1).priority))
            {
                System.out.println("Alt! Sbagliato!!!");System.exit(0);
            }
        }
        
            //We generate an SDFDebug and we write it in the log file via this *synchronized* method.
        SDFTaggerConfig.SDFLogger.writeInLogFile(firstSDFNode, ret);
        return ret;
    }
    
        //Create the bidirectional chain of SDFNode(s), one for each SDFHead of the input SDFDependencyTree(s). 
        //The methods returns the SDFRules to execute on the nodes. SDFRules (as well as the Bags on the nodes) are retrieved either from MongoDB
        //or from the XML files, depending on the SDFTaggerConfig.
        //We store in the Hashtable SDFHead2SDFNode the associations SDFHead->SDFNode; these are needed in SDFRule when we process the dependents 
        //or the governor: we move down|up to the dependents|governor (which is an SDFHead), but to check the constraints and, specifically, to enable
        //the movement in the four directions we need the SDFNode!
    protected Hashtable<SDFHead,SDFNode> SDFHead2SDFNode = new Hashtable<SDFHead,SDFNode>();
    private void buildSDFNodesChain(ArrayList<SDFDependencyTree> trees, SDFNode firstSDFNode) throws Exception
    {
        int index = 1;
        SDFNode lastSDFNode = firstSDFNode;
        
        for(int i=0;i<trees.size();i++)
        {
            for(int j=0;j<trees.get(i).getHeads().length;j++)
            {
                if(trees.get(i).getHeads()[j].getForm().compareToIgnoreCase("admin")==0)
                    i=i;

                boolean endSentence=false;
                if(j==trees.get(i).getHeads().length-1)endSentence=true;
                
                    //We create a new SDFNode and we fill its bags from the knowledge base
                SDFNode newSDFNode = new SDFNode(trees.get(i).getHeads()[j], index, new ArrayList<String>(), new ArrayList<String>(), endSentence);
                SDFTaggerConfig.KBManager.fillBagsOfSDFNode(newSDFNode.SDFHead.getForm(), newSDFNode.SDFHead.getLemma(), newSDFNode.bagsOnForm, newSDFNode.bagsOnLemma);
                SDFHead2SDFNode.put(newSDFNode.SDFHead, newSDFNode);
                
                    //Linking the new SDFNode in the chain.
                lastSDFNode.nextSDFNode = newSDFNode;
                newSDFNode.nextSDFNode = null;
                newSDFNode.prevSDFNode = lastSDFNode;
                firstSDFNode.prevSDFNode = newSDFNode; //firstSDFNode.prevNode is *always* the last node of the chain.
                lastSDFNode = newSDFNode;
                
                index++;            
            }
        }
    }
    
        //Binary insertion of tagsToAdd to the array. 
        //This method is synchronized, so that we can run the SDFRule(s) in multi-thread.
    private synchronized void addTags(ArrayList<SDFTag> array, ArrayList<SDFTag> tagsToAdd)
    {
        if(tagsToAdd.isEmpty())return;//if it's empty, we return immediately...
        
            //Tag(s) need to be returned ordered on the priority FROM THE HIGHEST TO THE LOWEST. All Tag(s) in tagsToAdd have the same priority, 
            //as they come from the same rule. We search the place of the first Tag via binary search, then we add them all together.
        int a = 0;
        int m = 0;
        int b = array.size()-1;

        while(b>=a)
        {
            m = (a+b)/2;
                
                //We found the place! we add the Tag(s) in array at m
            if(array.get(m).priority==tagsToAdd.get(0).priority)
            {
                for(int i=0;i<tagsToAdd.size();i++)array.add(m, tagsToAdd.get(i));
                return;
            }
            
                //If it is the latest element (but not the same), we have to create a new vector at this index (then we return).
                //This index can be either m or the one just after m.
            if((m==a)&&(m==b))
            {
                if(array.get(m).priority>tagsToAdd.get(0).priority)m++;
                for(int i=0;i<tagsToAdd.size();i++)array.add(m, tagsToAdd.get(i));
                return;
            }

                //binary search: we look for the place of key in the right half of the array
            if(array.get(m).priority>tagsToAdd.get(0).priority)a=m+1;
                //if m==a, we set b==a==m
            else if(m==a)b=a;else b=m-1;
        }
        
            //if we're here, the array is empty. We just add the tags.
        for(int i=0;i<tagsToAdd.size();i++)array.add(m, tagsToAdd.get(i));
    }
}