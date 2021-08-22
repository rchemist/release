# rcm-smb

**Rchemist SMB - based BLC4 Community edition**

Small and Medium Business Edition


## 실행 방법

1. local repository 에 git 을 연결하고 프로젝트 root 폴더에서 `mvn install` 실행

2. SpringBoot Application인 AdminApplication.java 를 run
 
 `vm option 으로 -javaagent:lib/spring-instrument-5.1.9.RELEASE.jar -javaagent:lib/aspectjweaver-1.9.4.jar` 추가 후 실행

3. SiteApplication.java 실행 


##  초기 Admin 비밀번호
- 아이디: admin
- 비밀번호: admin
초기 계정 정보를 변경하고 싶은 경우 Properties 로 `platform.config.admin.user.id` 와 `platform.config.admin.user.password` 값을 바꾸면 된다.


## Thymeleaf3 템플릿 작성 시 주의 사항

1. th:replace 대신 th:replace 사용
2. 파일 내용이 한 줄 짜리인 템플릿은 사용 불가
3. &lt;div /&gt; 와 같은 단일 태그 내 닫힘 태그 사용 불가


## Tech stack.

- Spring 5
- Spring boot 2
- Hibernate 5
- Thymeleaf 3
- Solr 6

## Module Projects

프로젝트를 구성하는 하위 모듈 프로젝트의 구조는 다음과 같다.

### smb-common
모든 모듈 프로젝트의 최상위 모듈이며, WAS나 DB, Site, Resources 등 서비스 구성에 필수적인 정보를 관리  

### smb-profile
Customer 관련 엔티티, 서비스 모듈. Customer 의 특성 상 smb-common 을 제외한 모든 모듈에 참조됨
+ 참조 모듈: smb-common

### smb-commerce
Catalog, Location, Inventory, Offer, Order 등 전자상거래 관련 기능이 집적된 모듈
+ 참조 모듈: smb-common, smb-profile 

### smb-cms
Front site 를 구성하는 CMS(ContentManagementSystem) 관련 엔티티, 서비스 모듈
+ 참조 모듈: smb-common, smb-profile

### smb-profile-web
Front Site 에서 Customer 와 관련된 Controller, Variable, Filter 등이 포함된 서비스 모듈
+ 참조 모듈: smb-common, smb-profile
  
### smb-event
Webhook 과 같은 비동기 이벤트 처리 서비스 모듈
+ 참조 모듈: smb-common, smb-cms, smb-commerce

### smb-batch
내장 배치 서비스 모듈
+ 참조 모듈: smb-commerce, smb-cms

### smb-framework
smb-framework-web, smb-framework-admin 에서 참조하도록 주요 모듈에 대한 패키징 모듈
+ 참조 모듈: smb-common, smb-commerce, smb-cms, smb-event

### smb-framework-web
Front Site 에서 참조하는 프레임워크 모듈

전자상거래, CMS, Customer 에 대한 모든 서비스 및 컨트롤러, 필터 등 사이트에 필요한 모든 정보가 포함되어 있음

+ 참조 모듈: smb-framework, smb-profile-web

### smb-framework-admin
Admin Site 에서 참조하는 프레임워크 모듈

BLC의 OpenAdmin 관련 엔티티, 서비스 모듈을 포함해 관리자도구 사이트를 위한 모든 서비스 및 컨트롤러, 필터 등이 포함되어 있음

+ 참조 모듈: smb-framework

### smb-admin
Admin Site

+ 참조 모듈: smb-framework-admin, smb-batch

### smb-api
Api Site
+ 참조 모듈: smb-framework-web

### smb-site
Front Site
+ 참조 모듈: smb-framework-web, smb-batch


## Github repository 설정

- mvn install -DskipTests
- mvn -DaltDeploymentRepository=snapshot::default::file:../github-release/snapshots clean deploy -DskipTests
- mvn -DaltDeploymentRepository=release::default::file:../github-release/releases clean deploy -DskipTests
- github-release repository를 commit 한다

## 스냅샷의 2일 초과한 데이터 제거

find ./snapshots/io/rchemist/smb-*/2.0.1-SNAPSHOT/smb-*.* -mtime +2 -type f -ls -exec rm {} \;
