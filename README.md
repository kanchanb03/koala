## Candy Inventory Dashboard

A simple single-page app  managing candy stock and capacity.


## Installation

 **Clone the repo**  
   ```bash
   git clone https://github.com/<your_username>/<your_repository>.git
   cd <your_repository>
````

 **Backend**

   ```bash
   cd api
   mvn clean package
   java -jar target/app.jar
   ```

 **Frontend**

   ```bash
   cd ../web
   npm install
   npm run build
   npm start
   ```

## Docker

   ```bash
   docker compose up --build
   ```

