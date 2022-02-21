package gitlet;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import static gitlet.Utils.*;
import static gitlet.Repository.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Represents a gitlet commit object.
 *  The Commit object represents a gitlet commit.
 *  Each commit tracks snaps of files that the user makes.
 *
 *  The Commit object also have various helper methods that are documented and explained below.
 *
 *  @author Ting-Che Lin
 */
public class Commit implements Serializable {

    private String message; //message of this commit
    private String id; // the unique SHA1 code for the commit
    private String parent; //the String reference to the id of the parent commit
    private HashMap<String, String> fileList;
    // A treemap containing all the files the commit is tracking
    private String timeStamp; //timestamp indicating when the commit was created
    private String mergeprt1;
    private String mergeprt2;


    public Commit(String msg, String prt, Stage stage) {
        message = msg;
        parent = prt;
        fileList = new HashMap<>();

        // Format date-time
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        Date date = new Date();
        timeStamp = formatter.format(date);
        //
        if (prt != null) {
            Commit parentCommit = readObject(join(COMMIT_DIR, prt), Commit.class);
            fileList = new HashMap<>();
            fileList.putAll(parentCommit.getFileList());
        }
        if (stage != null) {

            for (Map.Entry<String, String> entry
                    : stage.getRemovals().entrySet()) { //first remove files staged for removal
                fileList.remove(entry.getKey());
            }

            for (Map.Entry<String, String> entry
                    : stage.getFiles().entrySet()) { //update files
                fileList.put(entry.getKey(), entry.getValue());
            }
        }
        id = sha1(msg + prt + timeStamp + fileList);
    }

    public Commit(String msg, Stage stage, String prt1, String prt2) {
        this(msg, null, stage);
        this.parent = prt1;
        this.mergeprt1 = prt1;
        this.mergeprt2 = prt2;
    }


    // the getter methods that grants public access to private variables
    public String getMergeprt1() {
        return mergeprt1;
    }
    public String getMergeprt2() {
        return mergeprt2;
    }
    public String getMessage() {
        return message;
    }
    public String getTimestamp() {
        return timeStamp;
    }
    public String getParent() {
        return parent;
    }
    public void setParent(String prt) {
        parent = prt;
    }
    public HashMap<String, String> getFileList() {
        return fileList;
    }
    public String getId() {
        return id;
    }

    //

    public static void logCommits(Commit c) {
        System.out.println("===");
        System.out.println("commit " + c.getId());
        if (c.mergeprt1 != null) {
            System.out.println("Merge: " + c.mergeprt1.substring(0, 7)
                    + " " + c.mergeprt2.substring(0, 7));
        }
        System.out.println("Date: " + c.getTimestamp());
        System.out.println(c.getMessage());
        System.out.println();
        if (c.parent != null) {
            Commit prt = readObject(join(COMMIT_DIR, c.getParent()), Commit.class);
            logCommits(prt);
        }
    }

    public static void globalLogCommits() {
        List<String> commitList = plainFilenamesIn(COMMIT_DIR);
        for (String commit : commitList) {
            Commit c = readObject(join(COMMIT_DIR, commit), Commit.class);
            System.out.println("===");
            System.out.println("commit " + c.getId());
            if (c.mergeprt1 != null) {
                System.out.println("Merge: " + c.mergeprt1.substring(0, 7)
                        + " " + c.mergeprt2.substring(0, 7));
            }
            System.out.println("Date: " + c.getTimestamp());
            System.out.println(c.getMessage());
            System.out.println();
        }
    }

    public static boolean find(String s) {
        boolean hasMessage = false;
        List<String> commitList = plainFilenamesIn(COMMIT_DIR);
        for (String commit : commitList) {
            Commit c = readObject(join(COMMIT_DIR, commit), Commit.class);
            if (c.getMessage().equals(s)) {
                System.out.println(c.getId());
                hasMessage = true;
            }
        }
        return hasMessage;
    }
}
