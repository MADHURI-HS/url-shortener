window.onload = function () {

    const token = localStorage.getItem("jwt");

    if (!token) {
        window.location.href = "/login.html";
        return;
    }

    loadUrls();
};

const token = localStorage.getItem("jwt");

async function loadUrls() {

    try {

        const response = await fetch("/api/urls", {

            method: "GET",

            headers: {
                "Authorization": "Bearer " + token
            }

        });

        const result = await response.json();

        if (!response.ok) {
            console.error(result);
            return;
        }

        let html = "<h3>My URLs</h3>";

        if (result.data.length === 0) {

            html += "<p>No URLs created yet.</p>";

        } else {

           result.data.forEach(url => {

               html += `
                   <div style="margin-bottom:15px; padding:15px; border:1px solid #ddd; border-radius:8px;">

                       <strong>Short URL:</strong><br>

                       <a href="${window.location.origin}/${url.shortCode}" target="_blank">
                           ${window.location.origin}/${url.shortCode}
                       </a>

                       <br><br>

                       <strong>Original URL:</strong><br>

                       <small>${url.longUrl}</small>

                       <br><br>

                       <button onclick="deleteUrl('${url.shortCode}')">
                           Delete
                       </button>

                   </div>
               `;
           });

        }

        document.getElementById("result").innerHTML = html;

    } catch (error) {

        console.error("Load URLs Error:", error);

    }

}

async function shortenUrl() {

    const longUrl = document.getElementById("longUrl").value.trim();

    const message = document.getElementById("message");

    const shortResult = document.getElementById("shortResult");

    message.textContent = "";
    shortResult.innerHTML = "";

    if (!longUrl) {

        message.style.color = "red";
        message.textContent = "Please enter a URL.";

        return;
    }

    try {

        const response = await fetch("/api/shorten", {

            method: "POST",

            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },

            body: JSON.stringify({
                longUrl: longUrl
            })

        });

        const data = await response.json();

        if (response.ok) {

            message.style.color = "green";
            message.textContent = data.message;

            shortResult.innerHTML = `
                <p>
                    <strong>Short URL:</strong>
                    <a href="${data.data.shortUrl}" target="_blank">
                        ${data.data.shortUrl}
                    </a>
                </p>
            `;

            document.getElementById("longUrl").value = "";

            loadUrls();

        } else {

            message.style.color = "red";
            message.textContent = data.message;

        }

    } catch (error) {

        message.style.color = "red";
        message.textContent = "Server error.";

    }

}

function logout() {

    localStorage.removeItem("jwt");

    window.location.href = "/login.html";

}

async function deleteUrl(shortCode) {

    if (!confirm("Are you sure you want to delete this URL?")) {
        return;
    }

    try {

        const response = await fetch(`/api/urls/${shortCode}`, {

            method: "DELETE",

            headers: {
                "Authorization": "Bearer " + token
            }

        });

        const result = await response.json();

        if (response.ok) {

            alert(result.message);

            loadUrls();

        } else {

            alert(result.message);

        }

    } catch (error) {

        alert("Unable to delete URL.");

    }

}