/* =====================================================
GLOBAL VARIABLES
===================================================== */

var audio;
let currentEmotion = "";
let breathingRunning = false;
let breathingTimeout = null;
let currentCycle = 0;
let totalCycles = 5;

let progressBar;
let currentTimeText;
let durationText;
let isLooping = false;


/* =====================================================
SAFE ANDROID CALL
Prevents crashes if Android bridge is unavailable
===================================================== */

function callAndroid(method,...args){

    if(typeof Android !== "undefined" && Android[method]){
        Android[method](...args);
    }else{
        console.log("Android bridge missing:",method);
    }

}


/* =====================================================
APP STARTUP
===================================================== */

window.onload = function () {

    audio = document.getElementById("meditationAudio");

    progressBar = document.getElementById("progressBar");
    currentTimeText = document.getElementById("currentTime");
    durationText = document.getElementById("duration");

    if(progressBar){
        progressBar.addEventListener("input", seekAudio);
    }

    if(audio){
        audio.addEventListener("timeupdate", updateProgress);
    }

    /* Splash Screen Animation */

    const splash = document.getElementById("splash");

    if(splash){

        setTimeout(function(){

            splash.style.opacity = "0";

            setTimeout(function(){

                splash.style.display = "none";

                const app = document.getElementById("app");

                if(app){
                    app.classList.remove("hidden");
                    changeTab("home",0);
                }

            },900);

        },2500);

    }

    initScroll();
};


/* =====================================================
MUSIC PLAYER
===================================================== */

function togglePlay(){

const playIcon = document.querySelector(".play-main svg");

if(!audio || !playIcon) return;

if(audio.paused){

audio.play();

playIcon.innerHTML = `
<rect x="6" y="5" width="4" height="14" rx="1"></rect>
<rect x="14" y="5" width="4" height="14" rx="1"></rect>
`;

}else{

audio.pause();

playIcon.innerHTML = `
<path d="M8 5v14l11-7z"></path>
`;

}

}
function toggleLoop(){

    if(!audio) return;

    isLooping = !isLooping;

    audio.loop = isLooping;

    const loopBtn = document.querySelector(".loop-btn");

    if(loopBtn){

        if(isLooping){
            loopBtn.classList.add("active");
        }else{
            loopBtn.classList.remove("active");
        }

    }

}

function playTrack(file){

    if(!audio) return;

    audio.src = file;
    audio.load();

    audio.play().catch(function(e){
        console.log("Audio error:", e);
    });

    const icon = document.querySelector(".play-main svg");

    if(icon){
        icon.innerHTML = `
        <rect x="6" y="5" width="4" height="14" rx="1"></rect>
        <rect x="14" y="5" width="4" height="14" rx="1"></rect>
        `;
    }

    const track = document.getElementById("currentTrack");

    if(track){
        track.innerText = file.replace(".mp3","");
    }

}

function pauseMusic(){

    if(!audio) return;

    audio.pause();

    const track = document.getElementById("currentTrack");

    if(track){
        track.innerText = "Paused";
    }
}

function updateProgress(){

    if(!audio || !progressBar) return;
    if(!audio.duration) return;

    let progress = (audio.currentTime / audio.duration) * 100;

    progressBar.value = progress;

    if(currentTimeText){
        currentTimeText.innerText = formatTime(audio.currentTime);
    }

    if(durationText){
        durationText.innerText = formatTime(audio.duration);
    }

}

function seekAudio(){

    if(!audio || !progressBar) return;

    let seekTime = (progressBar.value / 100) * audio.duration;

    audio.currentTime = seekTime;

}

function formatTime(time){

    if(!time) return "0:00";

    let minutes = Math.floor(time / 60);
    let seconds = Math.floor(time % 60);

    if(seconds < 10){
        seconds = "0" + seconds;
    }

    return minutes + ":" + seconds;

}


/* =====================================================
BREATHING ANIMATION
===================================================== */

function toggleBreathing() {
    const btn = document.getElementById("breathingButton");
    const orb = document.getElementById("breathingCircle");
    const text = document.getElementById("breathingText");

    if (!btn || !orb || !text) return;

    if (!breathingRunning) {

        breathingRunning = true;
        currentCycle = 0;
        btn.innerText = "Stop Session";
        text.innerText = "Inhale slowly...";
        orb.classList.remove("expand");

        breatheCycle();

    } else {

        stopBreathing();
    }
}

function breatheCycle() {

    if (!breathingRunning) return;

    const orb = document.getElementById("breathingCircle");
    const text = document.getElementById("breathingText");

    if (!orb || !text) return;

    currentCycle++;

    text.innerText = "Inhale... (4)";
    orb.classList.add("expand");

    breathingTimeout = setTimeout(() => {

        if (!breathingRunning) return;

        text.innerText = "Hold... (3)";

        breathingTimeout = setTimeout(() => {

            if (!breathingRunning) return;

            text.innerText = "Exhale... (2)";
            orb.classList.remove("expand");

            breathingTimeout = setTimeout(() => {

                if (!breathingRunning) return;

                text.innerText = "Hold empty... (1)";

                breathingTimeout = setTimeout(() => {

                    if (currentCycle < totalCycles && breathingRunning){
                        breatheCycle();
                    }else{
                        completeSession();
                    }

                },1000);

            },2000);

        },1000);

    },2000);
}

function stopBreathing(){

    breathingRunning = false;

    if(breathingTimeout){
        clearTimeout(breathingTimeout);
    }

    const orb = document.getElementById("breathingCircle");
    const btn = document.getElementById("breathingButton");
    const text = document.getElementById("breathingText");

    if (orb) orb.classList.remove("expand");
    if (btn) btn.innerText = "Start Session";
    if (text) text.innerText = "Tap Start";

    currentCycle = 0;
}

function completeSession(){

    breathingRunning = false;

    const btn = document.getElementById("breathingButton");
    const text = document.getElementById("breathingText");

    if (btn) btn.innerText = "Start Session";
    if (text) text.innerText = "Session Complete! 🌿";

    currentCycle = 0;
}


/* =====================================================
DELETE HISTORY MODAL
===================================================== */

function openAccount(){
    changeTab("account",-1);

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}

function openHistory(){

    changeTab("history",-1); // don't activate nav item

    callAndroid("getChatHistory");
}

function openDeleteModal(){
    const modal = document.getElementById("deleteModal");
    if(modal) modal.classList.remove("hidden");
}

function closeDeleteModal(){
    const modal = document.getElementById("deleteModal");
    if(modal) modal.classList.add("hidden");
}

function confirmDeleteHistory(){
    callAndroid("deleteHistory");
    closeDeleteModal();

    setTimeout(()=>{ openHistory(); },500);
}


function resetMeditation(){

    stopBreathing();

    const emotionCard = document.getElementById("emotionResultCard");
    const meditationCard = document.getElementById("meditationCard");
    const breathingCard = document.getElementById("breathingCard");

    if(emotionCard) emotionCard.classList.add("hidden");
    if(meditationCard) meditationCard.classList.add("hidden");
    if(breathingCard) breathingCard.classList.add("hidden");

}


/* =====================================================
NAVIGATION SYSTEM
===================================================== */

function changeTab(screenId,index){

var screens = document.getElementsByClassName("screen");

for(var i=0;i<screens.length;i++){
screens[i].classList.remove("active");
}

var screen = document.getElementById(screenId);

if(screen){
screen.classList.add("active");
}

var items = document.getElementsByClassName("nav-item");

for(var i=0;i<items.length;i++){
items[i].classList.remove("active");
}

if(index >= 0 && items[index]){
items[index].classList.add("active");
}
window.scrollTo({
    top: 0,
    behavior: "instant"
});
var screen = document.getElementById(screenId);

if(screen){
screen.classList.add("active");
screen.scrollTop = 0;
}
}


/* =====================================================
AI MEDITATION ANALYSIS
===================================================== */

function sendToAndroid(){

    var input = document.getElementById("problem");

    if(!input) return;

    var problem = input.value.trim();

    if(!problem){
        alert("Please describe your problem 🌿");
        return;
    }

    callAndroid("analyzeEmotion",problem);
}

function showAIResult(emotion, meditation){

    currentEmotion = emotion;

    const emotionText = document.getElementById("detectedEmotion");
    if(emotionText) emotionText.innerText = emotion;

    const emotionCard = document.getElementById("emotionResultCard");
    const meditationCard = document.getElementById("meditationCard");
    const breathingCard = document.getElementById("breathingCard");

    if(emotionCard) emotionCard.classList.remove("hidden");
    if(meditationCard) meditationCard.classList.remove("hidden");
    if(breathingCard) breathingCard.classList.remove("hidden");

    const meditationText = document.getElementById("meditationText");

    if(meditationText){
        meditationText.innerText = meditation;
    }
}


/* =====================================================
THEME SYSTEM
===================================================== */

function toggleTheme(){
    document.body.classList.toggle("dark");
}


/* =====================================================
AUTHENTICATION
===================================================== */

function registerUser(){

    var name = document.getElementById("regName")?.value;
    var email = document.getElementById("regEmail")?.value;
    var password = document.getElementById("regPassword")?.value;

    callAndroid("registerUser",name,email,password);
}

function loginUser(){

    var email = document.getElementById("loginEmail")?.value.trim();
    var password = document.getElementById("loginPassword")?.value.trim();

    if(!email || !password){
        alert("Please enter email and password");
        return;
    }

    callAndroid("loginUser",email,password);
}

function logout(){
    callAndroid("logout");
}

/* =====================================================
CHAT HISTORY DISPLAY
===================================================== */

function showChatHistory(data){

    try{

        if(typeof data === "string"){
            data = JSON.parse(data);
        }

        const container = document.getElementById("historyContainer");

        if(!container) return;

        container.innerHTML = "";

        if(!data || data.length === 0){
            container.innerHTML = "<p>No chat history yet</p>";
            return;
        }

        data.forEach(function(item){

            const card = document.createElement("div");
            card.className = "history-item";

           card.innerHTML =
               "<div class='history-content'>" +
                   "<div class='history-emotion'>" + (item.emotion || "N/A") + "</div>" +
                   "<div class='history-text'>" + (item.message || "N/A") + "</div>" +
               "</div>" +

               "<button class='delete-chat-btn' onclick='deleteChat(" + item.id + ")'>🗑</button>";

            container.appendChild(card);

        });

    }catch(e){
        console.error("Chat history error:", e);
    }
}
function openSettings(){
    changeTab("settings",2);
}

function loadUsers(){

    console.log("Loading users...");

    if(typeof Android !== "undefined" && Android.getAllUsers){
        Android.getAllUsers();
    }else{
        console.log("Android bridge not ready");
    }
}

function loadStats(){

    console.log("Loading stats...");

    if(typeof Android !== "undefined" && Android.getStats){
        Android.getStats();
    }else{
        console.log("Android bridge not ready");
    }
}

function loadActivity(){

    console.log("Loading activity...");

    if(typeof Android !== "undefined" && Android.getUserActivity){
        Android.getUserActivity();
    }
}

function loadHighRisk(){

    console.log("Loading high risk users...");

    if(typeof Android !== "undefined" && Android.getHighRiskUsers){
        Android.getHighRiskUsers();
    }
}

/* =====================================================
ADMIN DASHBOARD DISPLAY
===================================================== */

function showStats(data){

    try{

        if(typeof data === "string"){
            data = JSON.parse(data);
        }

        const users = document.getElementById("totalUsers");
        const emotions = document.getElementById("totalEmotions");

        if(users) users.innerText = data.totalUsers || 0;
        if(emotions) emotions.innerText = data.totalEntries || 0;

    }catch(e){
        console.error("Stats error:", e);
    }

}


/* =====================================================
USERS TABLE
===================================================== */

function showUsers(data){

    try{

        if(typeof data === "string"){
            data = JSON.parse(data);
        }

        const table = document.getElementById("userTable");

        if(!table) return;

        table.innerHTML = `
        <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Email</th>
        <th>Action</th>
        </tr>
        `;

        data.forEach(function(user){

            const row = document.createElement("tr");

            row.innerHTML =
            "<td>"+user.id+"</td>"+
            "<td>"+user.name+"</td>"+
            "<td>"+user.email+"</td>"+
            "<td><button class='delete-btn' onclick='deleteUser("+user.id+")'>🗑</button></td>";;

            table.appendChild(row);

        });

    }catch(e){
        console.error("Users error:", e);
    }

}


/* =====================================================
USER ACTIVITY TABLE
===================================================== */

function showActivity(data){

    try{

        if(typeof data === "string"){
            data = JSON.parse(data);
        }

        const table = document.getElementById("activityTable");

        if(!table) return;

        table.innerHTML = `
        <tr>
        <th>Email</th>
        <th>Problem</th>
        <th>Emotion</th>
        </tr>
        `;

        data.forEach(function(item){

            const row = document.createElement("tr");

            row.innerHTML =
            "<td>"+item.email+"</td>"+
            "<td>"+item.problem+"</td>"+
            "<td>"+item.emotion+"</td>";

            table.appendChild(row);

        });

        renderEmotionChart(data);

    }catch(e){
        console.error("Activity error:", e);
    }

}


/* =====================================================
HIGH RISK USERS TABLE
===================================================== */

function showHighRisk(data){

    try{

        if(typeof data === "string"){
            data = JSON.parse(data);
        }

        const table = document.getElementById("riskTable");

        if(!table) return;

        table.innerHTML = `
        <tr>
        <th>Email</th>
        <th>Problem</th>
        <th>Emotion</th>
        </tr>
        `;

        data.forEach(function(item){

            const row = document.createElement("tr");

            row.innerHTML =
            "<td>"+item.email+"</td>"+
            "<td>"+item.problem+"</td>"+
            "<td style='color:red'>"+item.emotion+"</td>";

            table.appendChild(row);

        });

    }catch(e){
        console.error("Risk error:", e);
    }

}


/* =====================================================
DELETE USER
===================================================== */

function deleteUser(id){

    if(confirm("Delete this user?")){
        callAndroid("deleteUser",id);
    }

}

/* =====================================================
EMOTION ANALYTICS CHART
===================================================== */

function renderEmotionChart(data){

    try{

        const ctx = document.getElementById("emotionChart");

        if(!ctx) return;

        let counts = {};

        data.forEach(item => {

            if(!counts[item.emotion]){
                counts[item.emotion] = 0;
            }

            counts[item.emotion]++;

        });

        const labels = Object.keys(counts);
        const values = Object.values(counts);

        new Chart(ctx, {

            type: 'doughnut',

            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor:[
                        "#4F7CFF",
                        "#22c55e",
                        "#ef4444",
                        "#f59e0b",
                        "#a855f7"
                    ]
                }]
            },

            options:{
                plugins:{
                    legend:{
                        position:'bottom'
                    }
                }
            }

        });

    }catch(e){
        console.log("Chart error:",e);
    }

}
function openRegister(){
    callAndroid("openRegister");
}

/* =========================================
NAVBAR HIDE ON SCROLL
========================================= */

const navbar = document.querySelector(".navbar");
const screens = document.querySelectorAll(".screen");

screens.forEach(screen => {

let lastScroll = 0;

screen.addEventListener("scroll", function(){

const currentScroll = screen.scrollTop;

if(currentScroll > lastScroll && currentScroll > 40){

    // hide navbar
    navbar.style.transform = "translateY(120%)";

}else{

    // show navbar
    navbar.style.transform = "translateY(0)";
}

lastScroll = currentScroll;

});

});

function deleteHistory(){

    if(confirm("Delete entire chat history?")){

        callAndroid("deleteHistory");

    }

}

function historyCleared(){

    const list = document.getElementById("historyList");

    if(list){
        list.innerHTML = "<p class='empty'>No chat history</p>";
    }

}

function deleteChat(id){

    if(confirm("Delete this chat?")){
        callAndroid("deleteSingleChat", id);
    }

}



function goHome(){

    fetch("index.html")
        .then(res => res.text())
        .then(html => {
            document.getElementById("app").innerHTML = html;
        });

}

function loadPage(page){

    fetch(page)
        .then(res => res.text())
        .then(html => {

            const app = document.getElementById("app");

            if(app){
                app.innerHTML = html;
                window.scrollTo(0,0);
            }

        })
        .catch(err => console.log("Page load error:", err));
}

function loadHistory(){

    if(window.AndroidBridge){
        AndroidBridge.getChatHistory();
    }

}

function deleteHistory(){

    if(confirm("Delete all chat history?")){
        AndroidBridge.deleteHistory();
    }

}

function goBackToSettings(){
changeTab("settings",2);
}

