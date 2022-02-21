package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.Repository.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Ting-Che Lin
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        //empty command
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];

        //check if initialized
        if (!firstArg.equals("init") && !GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory");
            System.exit(0);
        }

        switch (firstArg) {
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);

            case "init":
                if(args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                if (GITLET_DIR.exists()) {
                    System.out.println("A Gitlet version-control system"
                          + " already exists in the current directory.");
                    System.exit(0);
                }
                setupPersistence();
                Commit initial = new Commit("initial commit", null, null);
                writeCommit(initial);
                break;

            case "add":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                }
                if (!join(CWD, args[1]).exists()) {
                    System.out.println("File does not exist.");
                    System.exit(0);
                }
                Stage stage2 = readObject(STAGE, Stage.class);
                stage2.addFile(args[1]);
                writeObject(STAGE, stage2);
                break;

            case "commit":
                Stage stage = readObject(STAGE, Stage.class);

                if (stage.getFiles().size() == 0
                        && stage.getRemovals().size() == 0) { //if no files are staged
                    System.out.println("No changes added to the commit.");
                    System.exit(0);
                }


                if (args[1].length() == 0) { // if no commit msg given
                    System.out.println("Please enter a commit message.");
                }

                Commit currentCommit = new Commit(args[1], getHeadCommit(), stage);
                writeCommit(currentCommit);
                stage.clearStage();
                stage.clearRemovals();
                writeObject(STAGE, stage);
                break;

            case "rm":
                Stage stage1 = readObject(STAGE, Stage.class);
                Commit rmCommit = readObject(join(COMMIT_DIR,getHeadCommit()), Commit.class);
                if (!stage1.getFiles().containsKey(args[1])
                        && !stage1.getRemovals().containsKey(args[1])
                            && !rmCommit.getFileList().containsKey(args[1])) {
                    System.out.println("No reason to remove the file.");
                    System.exit(0);
                }
                stage1.removeFile(args[1]);
                writeObject(STAGE, stage1);

                break;

            case "log":
                Commit c = readObject(join(COMMIT_DIR, getHeadCommit()), Commit.class);
                Commit.logCommits(c);
                break;

            case "global-log":
                Commit.globalLogCommits();
                break;

            case "find":
                if (!Commit.find(args[1])) {
                    System.out.println("Found no commit with that message");
                }
                break;

            case "status":
                //===========================================================
                System.out.println("=== Branches ===");
                List<String> branchesList = plainFilenamesIn(BRANCHES_DIR);
                for (String branch : branchesList) {
                    if (getCurrentBranch().equals(branch)) {
                        System.out.println("*" + branch);
                    } else {
                        System.out.println(branch);
                    }
                }
                System.out.println();
                //============================================================
                System.out.println("=== Staged Files ===");
                Stage stage3 = readObject(STAGE, Stage.class);
                for (Map.Entry<String, String> stagedFiles : stage3.getFiles().entrySet()) {
                    System.out.println(stagedFiles.getKey());
                }
                System.out.println();
                //=============================================================
                System.out.println("=== Removed Files ===");
                for (Map.Entry<String, String> removedFiles : stage3.getRemovals().entrySet()) {
                    System.out.println(removedFiles.getKey());
                }
                System.out.println();
                //=============================================================
                System.out.println("=== Modifications Not Staged For Commit === ");
                printModifiedFiles(getHeadCommit());
                System.out.println();
                //=============================================================
                System.out.println("=== Untracked Files ===");
                printUntrackedFiles(getHeadCommit());
                System.out.println();
                break;

            case "checkout":
                if (args[1].equals(getCurrentBranch()) && args.length == 2) {
                    System.out.println("No need to checkout the current branch.");
                    System.exit(0);
                }

                if (hasUntrackedFiles()) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }

                if (args.length == 3) {
                    //Failure case

                    Commit hc = readObject(join(COMMIT_DIR, getHeadCommit()), Commit.class);
                    if (!hc.getFileList().containsKey((args[2]))) {
                       System.out.println("File does not exist in that commit.");
                       System.exit(0);
                    }

                    if(!args[1].equals("--")){
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }

                    checkout(args[2], getHeadCommit());
                }

                else if (args.length == 4) {
                    //Failure cases
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }

                    String commitName = "";

                    if(args[1].length() < 40) {
                        List<String> filesInCommitDir = plainFilenamesIn(COMMIT_DIR);
                        for (String fileName : filesInCommitDir) {
                            if (fileName.startsWith(args[1])) {
                                commitName = fileName;
                            }
                        }
                    }
                    else {
                        commitName = args[1];
                    }

                    if(!join(COMMIT_DIR, commitName).exists()) {
                        System.out.println("No commit with that id exists.");
                        System.exit(0);
                    }

                    Commit hc = readObject(join(COMMIT_DIR, commitName), Commit.class);
                    if (!hc.getFileList().containsKey(args[3])) {
                        System.out.println("File does not exist in that commit.");
                        System.exit(0);
                    }

                    checkout(args[3], commitName);
                }

                else if (args.length == 2) {
                    if(!join(BRANCHES_DIR, args[1]).exists()) {
                        System.out.println("No such branch exists.");
                        System.exit(0);
                    }


                    String branchCommitID = readObject(join(BRANCHES_DIR, args[1]), String.class);

                    checkoutBranch(branchCommitID, args[1]);

                }
                else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;

            case "branch":
                if (join(BRANCHES_DIR, args[1]).exists()) {
                    System.out.println("A branch with that name already exists.");
                    System.exit(0);

                }
                Repository.createNewBranch(args[1]);
                writeObject(join(BRANCHES_DIR, args[1]), getHeadCommit());
                break;

            case "rm-branch":
                File rmbranch = join(BRANCHES_DIR, args[1]);
                if (!rmbranch.exists()) {
                    System.out.println("A branch with that name does not exist.");
                    System.exit(0);
                }
                if (getCurrentBranch().equals(args[1])) {
                    System.out.println("Cannot remove the current branch.");
                    System.exit(0);
                }
                rmbranch.delete();
                break;

            case "reset":
                if (!join(COMMIT_DIR, args[1]).exists()) {
                    System.out.println("No commit with that id exists");
                    System.exit(0);
                }
                if (hasUntrackedFiles() && !hasUncomittedChanges()) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                List<String> filesInCWD = plainFilenamesIn(CWD);
                Commit c1 = readObject(join(COMMIT_DIR, args[1]), Commit.class);

                for (String file : filesInCWD) {
                    if (c1.getFileList().containsKey(file)) {
                        checkout(file, args[1]);
                    } else {
                        restrictedDelete(file);
                    }
                }
                clearStage();
                setBranch(getCurrentBranch(), args[1]);
                break;
            case "merge":
                //failure cases
                if (hasUncomittedChanges()) {
                    System.out.println("You have uncommitted changes");
                    System.exit(0);
                }

                else if(!join(BRANCHES_DIR, args[1]).exists()) {
                    System.out.println("A branch with that name does not exist.");
                    System.exit(0);
                }
                else if (getCurrentBranch().equals(args[1])) {
                    System.out.println("Cannot merge a branch with itself.");
                    System.exit(0);
                }
                else if (hasUntrackedFiles()) {
                    System.out.println("There is an untracked file in "
                            + "the way; delete it, or add and commit it first.");
                    System.exit(0);
                }

                // failure cases end
                String branchCommitID = readObject(join(BRANCHES_DIR, args[1]), String.class);
                String commonAncestorID = findCommonAncestor(getHeadCommit(), getAllAncestors(branchCommitID));


                if (branchCommitID.equals(commonAncestorID)) {
                    System.out.println("Given branch is an ancestor of the current branch.");
                    System.exit(0);
                }
                else if (getHeadCommit().equals(commonAncestorID)) {
                    checkoutBranch(branchCommitID, getCurrentBranch());
                    System.out.println("Current branch fast-forwarded.");
                    System.exit(0);
                }

                Commit headCommit = readObject(join(COMMIT_DIR, getHeadCommit()), Commit.class);
                Commit branchCommit = readObject(join(COMMIT_DIR, branchCommitID), Commit.class);
                Commit commonAncestorCommit = readObject(join(COMMIT_DIR, commonAncestorID), Commit.class);

                Stage stage4 = readObject(STAGE, Stage.class);

                TreeSet<String> allFileNames = new TreeSet<>();
                HashMap<String,String> headFiles = headCommit.getFileList();
                HashMap<String,String> branchFiles = branchCommit.getFileList();


                allFileNames.addAll(headCommit.getFileList().keySet());
                allFileNames.addAll(branchCommit.getFileList().keySet());
                allFileNames.addAll(commonAncestorCommit.getFileList().keySet());

                for (String fileName : allFileNames) {
                    // case : file is present in all three
                    if (isPresentinCommit(fileName, headCommit)
                            && isPresentinCommit(fileName, branchCommit)
                            && isPresentinCommit(fileName, commonAncestorCommit)) {
                        //case 1
                        if (isDiffFromCommit(fileName, branchFiles.get(fileName), commonAncestorCommit) && !isDiffFromCommit(fileName, headFiles.get(fileName), commonAncestorCommit)) {
                            stage4.addExistingFile(fileName, branchFiles.get(fileName));
                        }

                        else if (isDiffFromCommit(fileName, headFiles.get(fileName), commonAncestorCommit) && !isDiffFromCommit(fileName, branchFiles.get(fileName), commonAncestorCommit)) {
                            stage4.addExistingFile(fileName, headFiles.get(fileName));

                        }

                        else if (isDiffFromCommit(fileName, headFiles.get(fileName), commonAncestorCommit) && isDiffFromCommit(fileName, branchFiles.get(fileName), commonAncestorCommit)) {
                            // case 3 part 1
                            if (headFiles.get(fileName).equals(branchFiles.get(fileName))) {
                                stage4.addExistingFile(fileName, headFiles.get(fileName));
                            }
                            else {
                                String mcFIle = mergeFiles(headFiles.get(fileName), branchFiles.get(fileName));
                                stage4.addExistingFile(fileName, mcFIle);
                            }
                        }
                    } else if (isPresentinCommit(fileName, headCommit) //present only in head
                            && !isPresentinCommit(fileName, branchCommit)
                            && !isPresentinCommit(fileName, commonAncestorCommit)) {
                        // case 4
                        stage4.addExistingFile(fileName, headFiles.get(fileName));
                        // case 6

                    } else if (!isPresentinCommit(fileName, headCommit) ///present only in branch
                            && isPresentinCommit(fileName, branchCommit)
                            && !isPresentinCommit(fileName, commonAncestorCommit)) {
                        //case 5
                        stage4.addExistingFile(fileName, branchFiles.get(fileName));
                        File f = join(CWD, fileName);
                        try {
                            f.createNewFile();
                        } catch (IOException e) {
                            System.out.println("Error");
                        }
                        writeContents(f, readContents(join(GITLET_DIR, branchFiles.get(fileName))));

                    } else if (isPresentinCommit(fileName, headCommit) ///present in head and ancestor
                            && !isPresentinCommit(fileName, branchCommit)
                            && isPresentinCommit(fileName, commonAncestorCommit)) {
                        if (!isDiffFromCommit(fileName, headFiles.get(fileName), commonAncestorCommit)) {
                            restrictedDelete(fileName);
                            // no need to rm
                        }

                        else {
                            String mcFIle = mergeFiles(headFiles.get(fileName), branchFiles.get(fileName));
                            stage4.addExistingFile(fileName, mcFIle);
                        }
                    } else if (!isPresentinCommit(fileName, headCommit) ///present in branch and ancestor
                            && isPresentinCommit(fileName, branchCommit)
                            && !isPresentinCommit(fileName, commonAncestorCommit)) {
                        if (!isDiffFromCommit(fileName, branchFiles.get(fileName), commonAncestorCommit)) {
                            stage4.addExistingFile(fileName, headFiles.get(fileName));
                        }
                        else {
                            String mcFIle = mergeFiles(headFiles.get(fileName), branchFiles.get(fileName));
                            stage4.addExistingFile(fileName, mcFIle);
                        }
                    } else if (isPresentinCommit(fileName, headCommit) ///present in branch and ancestor
                            && isPresentinCommit(fileName, branchCommit)
                            && !isPresentinCommit(fileName, commonAncestorCommit)) {

                        String mcFIle = mergeFiles(headFiles.get(fileName), branchFiles.get(fileName));
                        stage4.addExistingFile(fileName, mcFIle);
                    }

                }
                Commit mergeCommit = new Commit("Merged " + args[1] + " into " + getCurrentBranch() + ".",
                        stage4, getHeadCommit(), branchCommitID);
                writeCommit(mergeCommit);
                checkoutBranch(getHeadCommit(), getCurrentBranch());
        }
    }
}
