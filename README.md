## 초기 설정 
Toss PG API KEY 발급을 받아서 아래에 설정를 추가하여 application-payment-gateway.yml 파일을  
resources/ 폴더 아래에 생성한다.
- [토스 개발 페이지](https://developers.tosspayments.com/) 에 들어가서 발급 가능
```yml
# filename, application-payment-gateway.yml
payment:
  self:
    domain: http://localhost:8080
  toss:
    domain: https://api.tosspayments.com
    key:
      client: 
      secret: 
```