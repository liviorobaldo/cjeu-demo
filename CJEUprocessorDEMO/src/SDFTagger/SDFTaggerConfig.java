package SDFTagger;

import SDFTagger.KBInterface.KBInterface;
import java.io.File;

public abstract class SDFTaggerConfig 
{        
        //KBManager
    protected KBInterface KBManager = null;
    
        //SDFNodeConstraintsFactory for building the SDFRule(s)
    protected SDFNodeConstraints SDFNodeConstraintsFactory = null;
    
        //SDFLogger
    protected SDFLogger SDFLogger = null;
    
        //Files for the bags and the SDFRule(s)
    protected File rootDirectoryBags = null;
    protected String[] localPathsBags = null;
    protected File rootDirectoryXmlSDFRules = null;
    protected File rootDirectoryCompiledSDFRules = null;
    protected String[] localPathsSDFRules = null;
    
        //Empty constructor (empty logger by default)
    protected SDFTaggerConfig()throws Exception{SDFLogger=new SDFLogger(null);}
        //This constructor is useful in the subclasses, e.g. "compileKB", to create an SDFTaggerConfig which is a copy of another one
    protected SDFTaggerConfig(SDFTaggerConfig SDFTaggerConfig)
    {
        this.KBManager = SDFTaggerConfig.KBManager;
        this.SDFNodeConstraintsFactory = SDFTaggerConfig.SDFNodeConstraintsFactory;
        this.SDFLogger = SDFTaggerConfig.SDFLogger;
        this.rootDirectoryBags = SDFTaggerConfig.rootDirectoryBags;
        this.localPathsBags = SDFTaggerConfig.localPathsBags;
        this.rootDirectoryXmlSDFRules = SDFTaggerConfig.rootDirectoryXmlSDFRules;
        this.rootDirectoryCompiledSDFRules = SDFTaggerConfig.rootDirectoryCompiledSDFRules;
        this.localPathsSDFRules = SDFTaggerConfig.localPathsSDFRules;
    }
}
