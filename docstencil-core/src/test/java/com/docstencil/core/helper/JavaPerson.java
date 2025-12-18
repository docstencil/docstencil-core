package com.docstencil.core.helper;

@SuppressWarnings("unused")
public class JavaPerson {

  private String name;
  private int age;
  private double salary;

  public JavaPerson(final String name, final int age, final double salary) {
    this.name = name;
    this.age = age;
    this.salary = salary;
  }

  public String getName() {
    return name;
  }

  public JavaPerson setName(final String name) {
    this.name = name;
    return this;
  }

  public int getAge() {
    return age;
  }

  public JavaPerson setAge(final int age) {
    this.age = age;
    return this;
  }

  public double getSalary() {
    return salary;
  }

  public JavaPerson setSalary(final double salary) {
    this.salary = salary;
    return this;
  }
}
