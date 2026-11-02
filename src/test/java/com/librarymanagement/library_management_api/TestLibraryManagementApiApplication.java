package com.librarymanagement.library_management_api;

import org.springframework.boot.SpringApplication;

public class TestLibraryManagementApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(LibraryManagementApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
