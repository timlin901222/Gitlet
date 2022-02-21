package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.LinkedList;
import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  The gitlet Repository includes helper methods that helps get of modify files.
 *  does at a high level.
 *  @author Ting-Che Lin
 */
public class Repository implements Serializable {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    // The directory containing all branch files
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches_dir");

    // The file indicating the Head branch
    public static final File CURRENT_BRANCH = join(GITLET_DIR, "current_branch");

    // The file containing the stage object
    public static final File STAGE = Utils.join(GITLET_DIR, "staging_area");

    // The directory containing all commits
    public static final File COMMIT_DIR = Utils.join(GITLET_DIR, "commit_dir");

    public static void setupPersistence() {
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        try {
            STAGE.createNewFile();
            CURRENT_BRANCH.createNewFile();

        } catch (IOException excp) {
            System.out.println("Error in creating file.");
        }
        createNewBranch("master");
        setCurrentBranch("master");
        Stage stage = new Stage();
        writeObject(STAGE, stage);
    }

    public static void clearStage() {
        Stage stage = readObject(STAGE, Stage.class);
        stage.clearStage();
        writeObject(STAGE, stage);
    }

    public static void writeCommit(Commit commit) { //write a commit
        File comm = join(COMMIT_DIR, commit.getId());
        try {
            comm.createNewFile();
        } catch (IOException excp) {
            System.out.println("Error");
        }
        writeObject(comm, commit);
        setBranch(getCurrentBranch(), commit.getId());

    }

    public static void setBranch(String branchName, String commitId) {
        //update branch to a certain commit
        String saveData = commitId;
        writeObject(join(BRANCHES_DIR, branchName), saveData);
    }

    public static String getHeadCommit() {
        return readObject(
                join(BRANCHES_DIR, readObject(CURRENT_BRANCH, String.class)), String.class);

    }

    public static void setCurrentBranch(String branchName) {
        writeObject(CURRENT_BRANCH, branchName);
    }

    public static String getCurrentBranch() {
        return readObject(CURRENT_BRANCH, String.class);
    }

    public static void createNewBranch(String name) {
        File newBranch = join(BRANCHES_DIR, name);
        try {
            newBranch.createNewFile();
        } catch (IOException e) {
            System.out.println("Error");
        }

    }
    public static void checkout(String fileName, String commitID) {
        Commit headCommit = readObject(join(COMMIT_DIR, commitID), Commit.class);
        if (headCommit.getFileList().containsKey(fileName)) {
            byte[] contents
                    = readContents(join(GITLET_DIR, headCommit.getFileList().get(fileName)));
            if (!join(CWD, fileName).exists()) {
                File newFile = join(CWD, fileName);
                try {
                    newFile.createNewFile();
                } catch (java.io.IOException excp) {
                    System.out.println("Error");
                }
            }
            writeContents(join(CWD, fileName), contents);
        }
    }

    public static void checkoutBranch(String commitID, String branchName) {

        Commit branchCommit = readObject(join(COMMIT_DIR, commitID), Commit.class);

        List<String> fileList = plainFilenamesIn(CWD);

        for (String fileName : fileList) {
            if (branchCommit.getFileList().containsKey(fileName)) {
                String fileHashInGitDir = branchCommit.getFileList().get(fileName);
                byte[] contents = readContents(join(GITLET_DIR, fileHashInGitDir));

                writeContents(join(CWD, fileName), contents);
            } else {
                restrictedDelete(fileName);
            }
        }
        for (Map.Entry<String, String> entry : branchCommit.getFileList().entrySet()) {
            if (!join(CWD, entry.getKey()).exists()) {
                File newFile = join(CWD, entry.getKey());
                try {
                    newFile.createNewFile();
                } catch (IOException excp) {
                    System.out.println("Error");
                }
                writeContents(newFile, readContents(join(GITLET_DIR, entry.getValue())));

            }
        }
        writeObject(CURRENT_BRANCH, branchName);
    }

    public static TreeSet<String> getAllAncestors(String commitID) {
        TreeSet<String> ancestors = new TreeSet<>();
        LinkedList<String> queue = new LinkedList<>();
        queue.addLast(commitID);

        while (queue.size() != 0) {
            Commit c = readObject(join(COMMIT_DIR, queue.get(0)), Commit.class);
            if (c.getMergeprt1() != null) {
                queue.addLast(c.getMergeprt2());
                queue.addLast(c.getMergeprt1());
            } else if (c.getParent() != null) {
                queue.addLast(c.getParent());
            }
            ancestors.add(queue.get(0));
            queue.removeFirst();
        }
        return ancestors;
    }



    public static String findCommonAncestor(String commitID,
                                            TreeSet<String> ancestorsOtherBranch) {

        LinkedList<String> queue = new LinkedList<>();
        queue.addLast(commitID);
        while (queue.size() != 0) {
            Commit c = readObject(join(COMMIT_DIR, queue.getFirst()), Commit.class);
            if (ancestorsOtherBranch.contains(queue.getFirst())) {
                return queue.getFirst();
            } else if (c.getMergeprt1() != null) {
                queue.addLast(c.getMergeprt2());
                queue.addLast(c.getMergeprt1());

            } else if (c.getParent() != null) {
                queue.addLast(c.getParent());
            }
            queue.removeFirst();
        }
        return null;

    }

    public static boolean hasUncomittedChanges() {
        Stage stage = readObject(STAGE, Stage.class);
        if (stage.getFiles().size() != 0 || stage.getRemovals().size() != 0) {
            return true;
        }
        return false;
    }

    public static boolean hasUntrackedFiles() {
        List<String> filesInCWD = plainFilenamesIn(CWD);
        Commit c = readObject(join(COMMIT_DIR, getHeadCommit()), Commit.class);
        for (String file : filesInCWD) {
            if (!c.getFileList().containsKey(file)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDiffFromCommit(String fileName, String fileHash, Commit c) {
        if (!c.getFileList().get(fileName).equals(fileHash)) {
            return true;
        }
        return false;
    }

    public static boolean isPresentinCommit(String fileName, Commit c) {
        if (c.getFileList().containsKey(fileName)) {
            return true;
        }
        return false;
    }

    public static String mergeFiles(String hashIDCurrent, String hashIDOther) {
        String contentCurrent = "";
        String contentOther = "";
        if (hashIDCurrent != null) {
            contentCurrent = readContentsAsString(join(GITLET_DIR, hashIDCurrent));
        } else {
            contentCurrent = "";
        }

        if (hashIDOther != null) {
            contentOther = readContentsAsString(join(GITLET_DIR, hashIDOther));
        } else {
            contentOther = "";
        }
        return createMergeConflictFile(contentCurrent, contentOther);
    }

    public static String createMergeConflictFile(String content1, String content2) {
        String concatenatedContent = "<<<<<<< HEAD\n" + content1
                + "=======\n" + content2 + ">>>>>>>\n";
        String fileName = sha1(concatenatedContent);
        File mcFIle = join(GITLET_DIR, fileName);
        try {
            mcFIle.createNewFile();
        } catch (IOException e) {
            System.out.println("Error");
        }
        writeContents(mcFIle, concatenatedContent);
        System.out.println("Encountered a merge conflict.");
        return fileName;
    }


    public static void printUntrackedFiles(String commitID) {
        Commit c = readObject(join(COMMIT_DIR, commitID), Commit.class);
        List<String> filesInCWD = plainFilenamesIn(CWD);
        Stage stage = readObject(STAGE, Stage.class);
        for (String fileName : filesInCWD) {
            if (!c.getFileList().containsKey(fileName) && !stage.getFiles().containsKey(fileName)) {
                System.out.println(fileName);
            }
        }
    }

    public static void printModifiedFiles(String commitID) {
        Commit c = readObject(join(COMMIT_DIR, commitID), Commit.class);
        Stage stage = readObject(STAGE, Stage.class);
        for (Map.Entry<String, String> filesInCommit : c.getFileList().entrySet()) {
            if (!join(CWD, filesInCommit.getKey()).exists()
                    && !stage.getRemovals().containsKey(filesInCommit.getKey())) {
                System.out.println(filesInCommit.getKey() + " (deleted)");
                continue;
            }
            byte[] contents = readContents(join(CWD, filesInCommit.getKey()));
            String contentHash = sha1(contents);

            if (!filesInCommit.getValue().equals(contentHash)) {
                System.out.println(filesInCommit.getKey() + " (modified)");
            }

        }

    }
}
