package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

public class Stage implements Serializable {
    private TreeMap<String, String> files; //keys are file names, values are files hashes
    private TreeMap<String, String> removals; //key-value same as files
    public Stage() {
        files = new TreeMap<>();
        removals = new TreeMap<>();
    }

    public void addFile(String s) {
        if (files.size() != 0 && files.containsKey(s)) { //exits if file already in staging area
            System.exit(0);
        }

        byte[] snap = readContents(join(CWD, s));
        String snapName = Utils.sha1(snap);

        if (removals.containsKey(s)) { //if file is staged for removal, unstage it from removal
            removals.remove(s);
        } else {

            if (join(COMMIT_DIR, getHeadCommit()).exists()) {
                Commit headCommit
                        = Utils.readObject(join(COMMIT_DIR, getHeadCommit()), Commit.class);
                //See if the file is tracked by head commit
                if (headCommit.getFileList().containsKey(s)
                        && headCommit.getFileList().get(s).equals(snapName)) {
                    System.exit(0);
                }
            }

            File snapf = join(GITLET_DIR, snapName); // create a new snap file in .gitlet
            try {
                snapf.createNewFile();
            } catch (IOException excp) {
                System.out.println("Error");
            }

            writeContents(snapf, snap);
            files.put(s, snapName);
        }
    }

    public void addExistingFile(String fileName, String fileHash) {
        files.put(fileName, fileHash);
    }

    public void removeFile(String s) {
        Commit c = readObject(join(COMMIT_DIR, getHeadCommit()), Commit.class);
        String removal = files.remove(s);
        if (c.getFileList().containsKey(s)) {
            removals.put(s, removal);
            restrictedDelete(s);
        }

    }

    public void clearStage() {
        files.clear();
    }

    public void clearRemovals() {
        removals.clear();
    }

    public TreeMap<String, String> getFiles() {
        return files;
    }
    public TreeMap<String, String> getRemovals() {
        return removals;
    }
}
