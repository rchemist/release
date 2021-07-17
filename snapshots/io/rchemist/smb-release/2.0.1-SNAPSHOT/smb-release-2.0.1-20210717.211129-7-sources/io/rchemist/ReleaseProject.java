/*
 *  Copyright (c) 2021. rchemist.io by Rchemist
 *  Licensed under the Rchemist Common License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License under controlled by Rchemist
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.rchemist;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 프로젝트를 github 으로 release 할 때 사용한다
 * <p>
 * <p>
 * Project : Rchemist Commerce Platform
 * <p>
 * Created by kunner
 *
 * </p>
 **/
public class ReleaseProject {

  // release 할 프로젝트 root - main argument 로 root=/Users/kunner/.....
  private static final String ROOT_DIR = "/Users/kunner/IdeaProjects/rcm-smb";

  // release 된 pom, jar 가 전송될 디렉토리 명 - 현재 root 와 동등한 레벨의 프로젝트라고 간주한다, main argument 로 target=디렉토리명
  private static final String TARGET_DIR = "release";

  // argument 의 구분자. root=path정보 target=디렉토리명 since=60 release=snapshot
  private static final String ARG_SPLIT = "=";

  // release 로 내보낼 것인지 snapshot 으로 보낼 것인지, 둘 다 할 것인지(both)
  // release=release    release=snapshot    release=both
  private static final ReleaseType RELEASE_TYPE = ReleaseType.SNAPSHOT;

  // 최근 몇분 동안을 기준으로 할 것인지 - 시간 체크를 하지 않을 거면 이 클래스를 쓰지 말아야 한다
  private static final int LAST_MODIFIED_MINUTE_AGO = 3600;

  // 넘겨 받은 arguments 를 유효한 것만 모아 놓은 것
  private static final Map<String, String> ARGUMENTS = new HashMap<>();

  // 최근 지정된 시간 동안 변경된 프로젝트명
  private static final Set<String> CHANGED_PROJECTS = new HashSet<>();

  // 변경 감지할 때 무시할 파일 확장자
  private static final String[] IGNORE_POSTFIXES = new String[]{"iml", "idea", "pid", "git", "gitignore"};

  // 변경 감지할 때 무시할 폴더명
  private static final String[] IGNORE_FOLDERS = new String[]{"target", "release", ".idea", "out", ".DS_Store"};

  /**
   * 실행 메소드
   * @param args
   */
  public static void main(String[] args) {

    // arguments 정제
    parseArguments(args);

    // 실행 기준 폴더 - 프로젝트 root
    File root = (ARGUMENTS.containsKey("root")) ? new File(ARGUMENTS.get("root")) : new File(ROOT_DIR);
    // loop 를 돌 때 최상위 프로젝트 정보를 기억하기 위해 할당
    String absolutePath = root.getAbsolutePath();

    // sinceTimeStamp 를 구하기 위해 최근 X분 이내 값을 찾는다
    int lastModifiedMinuteAgo = (ARGUMENTS.containsKey("since")) ? Integer.valueOf(ARGUMENTS.get("since")) : LAST_MODIFIED_MINUTE_AGO;
    long sinceTimeStamp = Timestamp.valueOf(LocalDateTime.now().minusMinutes(lastModifiedMinuteAgo)).getTime();

    // 대상 프로젝트 폴더를 순회 하면서 해당 기간 내 변경된 파일이 있는지 확인한다.
    // 변경됐다고 확인된 프로젝트 폴더는 CHANGED_PROJECTS 에 저장한다.
    getChangedDirectories(root, absolutePath, sinceTimeStamp);

    // maven clean install -DskipTests
    // mvnBuild(root);

    // release project 를 pull 하는 것으로 시작
    pullReleaseProject(root);

    if(CHANGED_PROJECTS.size() == 0){
      System.out.println("No files added in " + lastModifiedMinuteAgo + " minutes.");
      System.out.println("Build process ended.");
    }else{

      // build first
      // mvnBuild(root);

      // then, release
      ReleaseType releaseType = (ARGUMENTS.containsKey("release")) ? ReleaseType.get(ARGUMENTS.get("release")) : RELEASE_TYPE;
      for(String project : CHANGED_PROJECTS){

        File folder = new File(root.getAbsolutePath() + "/" + project);

        if(releaseType.equals(ReleaseType.RELEASE) || releaseType.equals(ReleaseType.BOTH)){
          // releases build
          release(folder, true);
        }

        if(releaseType.equals(ReleaseType.SNAPSHOT) || releaseType.equals(ReleaseType.BOTH)){
          // snapshot build
          release(folder,false);
        }
      }
    }

  }

  protected static void pullReleaseProject(File root){

    String releaseProject = ARGUMENTS.containsKey("target") ? ARGUMENTS.get("target") : TARGET_DIR;

    String releaseRoot = StringUtils.substringBeforeLast(root.getAbsolutePath(), "/") + "/" + releaseProject;

    process(new ProcessBuilder("git", "pull").directory(new File(releaseRoot)), true);


  }

  protected static void release(File folder, boolean release){

    String releaseProject = ARGUMENTS.containsKey("target") ? ARGUMENTS.get("target") : TARGET_DIR;

    String arg = (release) ?
        "-DaltDeploymentRepository=release::default::file:../../"+releaseProject+"/releases"
        : "-DaltDeploymentRepository=snapshot::default::file:../../"+releaseProject+"/snapshots";    // 기준점이 project 별 디렉토리라서 ../../ 두칸 내려간다.

    process(new ProcessBuilder("mvn", arg, "clean", "deploy", "-DskipTests").directory(folder), false);

  }

  protected static void getChangedDirectories(File file, String absolutePath, long sinceTimeStamp){
    File files[] = file.listFiles();

    for(int i = 0; i < files.length; i++){

      File now = files[i];

      if(now.isDirectory()){
        if(StringUtils.equalsAny(now.getName(), IGNORE_FOLDERS)){
          continue;
        }
      }else{

        String filename = now.getName();
        if(StringUtils.contains(filename, ".")){
          String postFix = StringUtils.substringAfterLast(filename, ".");
          if(ArrayUtils.contains(IGNORE_POSTFIXES, postFix)){
            continue;
          }
        }
      }

      String subDirectory = StringUtils.substringAfter(now.getAbsolutePath(), absolutePath);
      if(subDirectory.startsWith("/")){
        subDirectory = StringUtils.substringAfter(subDirectory, "/");
      }

      if(!now.isDirectory()){
        subDirectory = StringUtils.substringBeforeLast(subDirectory, now.getName());
      }

      String projectRoot = (subDirectory.contains("/")) ? StringUtils.substringBefore(subDirectory, "/") : subDirectory;

      if(StringUtils.isEmpty(projectRoot) || !CHANGED_PROJECTS.contains(projectRoot)){
        File subFile = now;
        if(subFile.isDirectory()){
          if(!subFile.getName().equals("target")){
            getChangedDirectories(subFile, absolutePath, sinceTimeStamp);
          }
        }else{
          // 어차피 root 는 검사 대상이 아니다
          if(now.lastModified() > sinceTimeStamp && StringUtils.isNotEmpty(projectRoot) && !StringUtils.startsWith(projectRoot, ".")){
            CHANGED_PROJECTS.add(projectRoot);
            System.out.println("Directory " + projectRoot + " has added.");
            System.out.println("Due to " + now.getAbsolutePath() + "/" + now.getName());
          }

        }
      }
    }

  }


  protected static void parseArguments(String[] args){
    if(ArrayUtils.isNotEmpty(args)){
      for (String arg : args) {
        if (StringUtils.contains(arg, ARG_SPLIT)) {
          String key = StringUtils.substringBefore(arg, ARG_SPLIT);
          String value = StringUtils.substringAfter(arg, ARG_SPLIT);
          ARGUMENTS.put(key, value);
        }else{
          if(StringUtils.equalsAnyIgnoreCase(arg, "snapshot", "releases", "both")){
            ARGUMENTS.put("release", arg);
          }
        }
      }
    }
  }


  protected static void mvnBuild(File dir){
    process(new ProcessBuilder("mvn", "clean", "install", "-DskipTests").directory(dir), false);
  }

  protected static String process(ProcessBuilder builder, boolean getString){
    Process p = null;
    try {
      p = builder.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
    StringBuffer sb = new StringBuffer();
    BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
    try {
      String line;
      while ( (line = output.readLine()) != null) {
        System.out.println(line);
        if(getString){
          sb.append(line);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return (getString) ? sb.toString() : null;

  }

  private enum ReleaseType{
    SNAPSHOT, RELEASE, BOTH;

    static ReleaseType get(String str){
      return ReleaseType.valueOf(str.toUpperCase());
    }

  }

}