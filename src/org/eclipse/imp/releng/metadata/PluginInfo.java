package org.eclipse.imp.releng.metadata;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.imp.releng.ReleaseEngineeringPlugin;

public class PluginInfo {
    public static abstract class ChangeReason {
	public abstract boolean isChange();
    }

    private static class NoChange extends ChangeReason {
	private static final NoChange sInstance= new NoChange();
	public static final NoChange getInstance() { return sInstance; }
	private NoChange() { }

	@Override
	public String toString() {
	    return "<unchanged>";
	}

	@Override
	public boolean isChange() {
	    return false;
	}
    }

    public static class NewPluginChange extends ChangeReason {
	private static final NewPluginChange sInstance= new NewPluginChange();
	public static final NewPluginChange getInstance() { return sInstance; }
	private NewPluginChange() { }

	@Override
	public String toString() {
	    return "<new plugin>";
	}

	@Override
	public boolean isChange() {
	    return true;
	}
    }

    public abstract static class ResourceChange extends ChangeReason {
	protected final IPath fPath;

	public ResourceChange(IPath path) {
	    fPath= path;
	}
	public IPath getPath() {
	    return fPath;
	}
	@Override
	public boolean isChange() {
	    return true;
	}
	public abstract String getType();
    }

    public static class FileChange extends ResourceChange {
	public FileChange(IPath path) {
	    super(path);
	}
	@Override
	public String toString() {
	    return "<file change: " + fPath.toPortableString() + ">";
	}
	@Override
	public String getType() {
	    return "<changed>";
	}
    }

    public static class FileDeleted extends ResourceChange {
	public FileDeleted(IPath path) {
	    super(path);
	}
	@Override
	public String toString() {
	    return "<file deleted: " + fPath.toPortableString() + ">";
	}
	@Override
	public String getType() {
	    return "<deleted>";
	}
    }

    public static class FileAdded extends ResourceChange {
	public FileAdded(IPath path) {
	    super(path);
	}
	@Override
	public String toString() {
	    return "<file added: " + fPath.toPortableString() + ">";
	}
	@Override
	public String getType() {
	    return "<added>";
	}
    }

    public IFile fManifest;

    public String fPluginID;

    public String fPluginVersion;

    public String fPluginNewVersion;

    public FileVersionMap fCurMap;

    public FileVersionMap fNewMap;

    private boolean fPluginOk= true;

    private ChangeReason fChanged= NoChange.getInstance();

    private List<ChangeReason> fAllChanges= new ArrayList<ChangeReason>();

    public PluginInfo(String pluginID, String newVersion) {
	fPluginID= pluginID;
	fPluginVersion= newVersion;

	IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();

	if (wsRoot.getProject(pluginID) != null) {
	    fManifest= wsRoot.getProject(pluginID).getFile(new Path("META-INF/MANIFEST.MF"));
	    if (!fManifest.exists()) {
		ReleaseEngineeringPlugin.getMsgStream().println("     * Unable to find bundle manifest for plugin " + pluginID);
		fPluginOk= false;
	    }
	} else {
	    ReleaseEngineeringPlugin.getMsgStream().println("     * Unable to find project for plugin " + pluginID);
	    fPluginOk= false;
	}
    }

    public ChangeReason getChangeState() {
	return fChanged;
    }

    public List<ChangeReason> getAllChanges() {
	return fAllChanges;
    }

    public void updateReason(ChangeReason reason) {
	fAllChanges.add(reason);
	if (fChanged instanceof NewPluginChange) {
	    // do nothing; this is the most potent reason for bumping the version
	} else if (fChanged instanceof NoChange) {
	    fChanged= reason;
	} else if (reason instanceof FileAdded) {
	    fChanged= reason;
	} else if (reason instanceof FileChange && !(fChanged instanceof FileAdded)) {
	    fChanged= reason;
	} // else the change must be a FileDeleted
    }

    public boolean pluginOk() {
	return fPluginOk;
    }

    @Override
    public String toString() {
        return fPluginID;
    }
}
