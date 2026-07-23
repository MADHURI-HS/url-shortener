const registerForm = document.getElementById("registerForm");
const loginForm = document.getElementById("loginForm");

if (registerForm) {
    registerForm.addEventListener("submit", registerUser);
}

if (loginForm) {
    loginForm.addEventListener("submit", loginUser);
}

async function registerUser(event) {

    event.preventDefault();

    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();

    const message = document.getElementById("message");

    try {

        const response = await fetch("/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name,
                email,
                password
            })
        });

        const result = await response.json();

        if (response.ok) {

            message.style.color = "green";
            message.innerText = result.message;

            setTimeout(() => {
                window.location.href = "/login.html";
            }, 1500);

        } else {

            message.style.color = "red";
            message.innerText = result.message;
        }

    } catch (error) {

        message.style.color = "red";
        message.innerText = "Unable to connect to server.";
    }
}

async function loginUser(event) {

    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();

    const message = document.getElementById("message");

    try {

        const response = await fetch("/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email,
                password
            })
        });

        const result = await response.json();

        if (response.ok) {

            localStorage.setItem("jwt", result.data.token);

            message.style.color = "green";
            message.innerText = result.message;

            setTimeout(() => {
                window.location.href = "/dashboard.html";
            }, 1000);

        } else {

            message.style.color = "red";
            message.innerText = result.message;
        }

    } catch (error) {

        message.style.color = "red";
        message.innerText = "Unable to connect to server.";
    }
}