package com.example.demo.Profil.Controller;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.example.demo.Profil.models.User;
import com.github.javafaker.Faker;

import org.apache.tomcat.util.json.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@EnableAutoConfiguration
@RequestMapping("/api")
public class UserController {
    SimpleDateFormat dbirth = new SimpleDateFormat("MM-dd-yyyy");
    // Generate users
    @RequestMapping(value="/users/generate/{count}", method = RequestMethod.GET, consumes = "application/json")
    public ResponseEntity<Object> generate_user(@PathVariable("count") int count){
        String[] role = {"admin", "user"};
        JSONObject principal_json = new JSONObject();
        Faker faker = new Faker();
        String text= "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz%*£$_-@&";
        Random random = new Random();
        JSONArray data_structure = new JSONArray();
        for( int i=0; i<count;i++){
            int nbr_pswr = random.nextInt(4 + 1) + 6;
            StringBuilder password_builder = new StringBuilder();
            for (int j=0; j< nbr_pswr; j++){
                int index = random.nextInt(text.length());
                password_builder.append(text.charAt(index));
            }    
            JSONObject json_user = new JSONObject();
            json_user.put("firstName", faker.name().firstName());
            json_user.put("lastName", faker.name().lastName());
            json_user.put("birthDate", dbirth.format(faker.date().birthday()));
            json_user.put("city", faker.address().city());
            json_user.put("country", faker.address().countryCode());
            json_user.put("avatar", faker.internet().avatar());
            json_user.put("company", faker.company().name());
            json_user.put("jobPosition", faker.job().position());
            json_user.put("mobile", faker.phoneNumber().cellPhone());
            json_user.put("username", faker.name().username());
            json_user.put("email", faker.internet().emailAddress());
            json_user.put("password",password_builder.toString());
            json_user.put("role", role[random.nextInt(role.length)]);
            data_structure.add(json_user);
            
            
        }
        principal_json.put("users",data_structure);
        try {
            FileWriter file = new FileWriter("./src/main/resources/static/downloaded/generated_users.json");
            file.write(principal_json.toJSONString());
            file.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        
        return new ResponseEntity<>("Check the static folder in the present project.", HttpStatus.OK);
    }
    // Upload users
    @Autowired
    private UserRepository user_repository;
    private PasswordEncoder password_encoder;
    @RequestMapping(value="users/batch", method=RequestMethod.POST, consumes = {"multipart/form-data"})
    public JSONObject upload_users(@RequestParam("file") MultipartFile file) throws IllegalStateException, IOException, ParseException, org.json.simple.parser.ParseException, java.text.ParseException {
        this.password_encoder = new BCryptPasswordEncoder();
        File convFile = new File("./src/main/resources/static/uploaded/" + file.getOriginalFilename() );
        FileOutputStream fos = new FileOutputStream( convFile );
        fos.write( file.getBytes() );
        FileReader file_reader = new FileReader(convFile.getAbsolutePath());
        JSONParser data_test = new JSONParser();
        Object obj = data_test.parse(file_reader);
        JSONObject usersobj = (JSONObject) obj;
        JSONArray users_array = (JSONArray) usersobj.get("users");
        ArrayList <String> usernames = new ArrayList<String>();
        ArrayList <String> emails = new ArrayList<String>();
        ArrayList <String> duplicated_usernames = new ArrayList<String>();
        ArrayList <String> duplicated_emails = new ArrayList<String>();
        int created_user = 0;
        int canceled = 0;
        for (int i=0; i<users_array.size(); i++){
            JSONObject user = (JSONObject) users_array.get(i);
            User check_db = user_repository.findByUsername((String)user.get("username"));
            if (usernames.contains(user.get("username")) || emails.contains(user.get("email")) || check_db != null){
                canceled++;
                if (!duplicated_emails.contains(user.get("username"))){
                    
                    duplicated_usernames.add((String) user.get("username"));
                }
                if (!duplicated_emails.contains(user.get("email"))){

                    duplicated_emails.add((String) user.get("email"));
                }
            }
            else{
                usernames.add((String) user.get("username"));
                emails.add((String) user.get("email"));
                String password = this.password_encoder.encode((String)user.get("password"));
                User this_user = new User();
                this_user.setFirstName((String)user.get("firstName")); 
                this_user.setLastName((String)user.get("lastName")); 
                this_user.setBirthDate(dbirth.parse(user.get("birthDate").toString()));
                this_user.setCity((String) user.get("city"));
                this_user.setCountry((String) user.get("country"));
                this_user.setAvatar((String) user.get("avatar"));
                this_user.setCompany((String) user.get("company"));
                this_user.setJobPosition((String) user.get("jobPosition"));
                this_user.setMobile((String) user.get("mobile"));
                this_user.setUsername((String) user.get("username"));
                this_user.setEmail((String) user.get("email"));
                this_user.setPassword(password);
                this_user.setRole((String) user.get("role"));
                user_repository.save(this_user);
                created_user++;
            }   
        }
        file_reader.close();
        fos.close();
        if (convFile.delete()){
            System.out.println("Deleted");
        }
        else{
            System.out.println("Can't be deleted");
        }
        JSONObject results = new JSONObject();
        results.put("Created_users", created_user);
        results.put("Canceled", canceled);
        results.put("Duplicated_emails", duplicated_emails);
        results.put("Duplicated_usernames", duplicated_usernames);
        

        return results;
    }
    // Authentication 
    private String  secret = "mini_project";
    @RequestMapping(value="/auth", method=RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<Object> login(@RequestBody JSONObject user) {
        String  username = (String)user.get("username");
        User user_details;
        if (username.contains("@")){
            user_details = user_repository.findByEmail((String) user.get("username"));
            System.out.println("Connection by email");
        }
        else{
            System.out.println("Connection by username");
            user_details = user_repository.findByUsername((String) user.get("username"));
        }
        JSONObject result = new JSONObject();
        if (user_details != null){
            if(this.password_encoder.matches((String)user.get("password"), user_details.getPassword())){
                System.out.println("Correct password");
                Map<String, Object> claims = new HashMap<>();
                String token = token(claims, user_details.getEmail());
                result.put("accessToken", token);
                return new ResponseEntity<>(result, HttpStatus.OK);
            }else{
                System.out.println("incorrect password");;
                result.put("accessToken", "incorrect password");
                return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
            }
            
        }
        else{
            result.put("accessToken", "No user with this username.");
            System.err.println("No user with this username.");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
        
    }
    // To generate a token
    public String token(Map<String, Object> claims, String subject){
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 *20))
        .signWith(SignatureAlgorithm.HS256, secret).compact();
    }
    // Consult my  profil
    @RequestMapping(value = "/users/me", method = RequestMethod.GET, consumes = "application/json")
    public ResponseEntity<Object> my_profil(@RequestBody JSONObject accessToken){
        try{ // verify Token experation
            String token = (String) accessToken.get("accessToken");
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            User user_details;
            String username = claims.getSubject();
            user_details = user_repository.findByEmail(username);
            return new ResponseEntity<>(user_details,HttpStatus.OK);
        }catch (ExpiredJwtException e){
            return new ResponseEntity<>("ExpiredToken!!!!",HttpStatus.BAD_REQUEST);
        }
            
    }


    // Consult Profil of another user
    @RequestMapping(value="/users/{profil}", method=RequestMethod.GET, consumes = "application/json")
    public ResponseEntity<Object> get_profil(@PathVariable("profil") String profil, @RequestBody JSONObject accessToken) {
        try{
            String token = (String) accessToken.get("accessToken");
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            String email = claims.getSubject();
            User current_user = user_repository.findByEmail(email);
            String username = current_user.getUsername();
            if (username.equals(profil) ){
                return new ResponseEntity<>(current_user,HttpStatus.OK);
            }
            else{
                if (current_user.getRole().equals("admin")){
                    User user_details = user_repository.findByUsername(profil);
                    if (user_details == null){
                        return new ResponseEntity<>("There is no user with this username!",HttpStatus.BAD_REQUEST);
                    }
                    else{
                        return new ResponseEntity<>(user_details,HttpStatus.OK);
                    }
                }
                else{
                    return new ResponseEntity<>("You don't have access to this user data!",HttpStatus.BAD_REQUEST);
                }
            }
        }catch (ExpiredJwtException e){
            return new ResponseEntity<>("ExpiredToken!!!!",HttpStatus.BAD_REQUEST);
        }
    }
    
}
