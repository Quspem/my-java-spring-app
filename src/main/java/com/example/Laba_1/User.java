package com.example.Laba_1;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;

    @NotBlank(message = "Имя обязательно, минимум 2 символа")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Неверный формат email")
    @Indexed(unique = true)
    private String email;

    @NotBlank
    @Pattern(regexp = "^ББМО-\\d{2}-\\d{2}$", message = "Формат должен быть ББМО-XX-XX")
    private String group_name;

    @Min(value = 16, message = "Возраст от 16 до 100")
    @Max(value = 100)
    private int age;

    @Min(1) @Max(4)
    private int course;

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGroup_name() { return group_name; }
    public void setGroup_name(String group_name) { this.group_name = group_name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public int getCourse() { return course; }
    public void setCourse(int course) { this.course = course; }
}