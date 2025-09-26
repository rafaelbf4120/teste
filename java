// Importações do Firebase
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.1/firebase-app.js";
import { getAuth, signInAnonymously, onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/11.6.1/firebase-auth.js";
import { getFirestore, addDoc, deleteDoc, onSnapshot, collection, doc, query, where, getDocs, setLogLevel } from "https://www.gstatic.com/firebasejs/11.6.1/firebase-firestore.js";

// Firebase Config - Substitua com a sua própria configuração
const firebaseConfig = {
    apiKey: "AIzaSyDmqvcKtIsga4ZQWNDg4_2k493dqMQCDVg",
    authDomain: "teste-ebf38.firebaseapp.com",
    projectId: "teste-ebf38",
    storageBucket: "teste-ebf38.firebasestorage.app",
    messagingSenderId: "741884776297",
    appId: "1:741884776297:web:a23450b4909581a1b237f8",
    measurementId: "G-2MD5CFD51E"
};

// Inicializa o Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);
setLogLevel('debug');

const globalAppId = typeof __app_id !== 'undefined' ? __app_id : 'default-app-id';
let globalUserId = null;

// Elementos do DOM
const loginForm = document.getElementById('login-form');
const loginPage = document.getElementById('login-page');
const appPage = document.getElementById('app-page');
const loginMessage = document.getElementById('login-message');
const userIdDisplay = document.getElementById('user-id-display');
const logoutButton = document.getElementById('logout-btn');
const matriculaInput = document.getElementById('matricula');
const transportadoInput = document.getElementById('transportado');
const valorInput = document.getElementById('valor');
const valorExtraInput = document.getElementById('valor-extra');
const messageModal = document.getElementById('message-modal');
const closeModalButton = document.getElementById('close-modal');

// Lista de usuários e senhas (pode ser expandida)
const users = [
    { username: 'admin', password: 'rafael22' },
    { username: 'gerente', password: 'senha123' }
];

let transportadosData = [];
let motoristasData = [];

// Mapeamentos para preenchimento automático
let matriculaToNome = {};
let nomeToMatricula = {};

// Funções para gerenciar o modal de aviso
function showWarning(message) {
    document.getElementById('message-content').innerText = message;
    messageModal.classList.remove('hidden');
}

function hideWarning() {
    messageModal.classList.add('hidden');
}

// Funções de formatação de moeda
function formatCurrencyInput(input) {
    let value = input.value.replace(/\D/g, ''); // Remove todos os caracteres não numéricos
    if (value === '') {
        return;
    }
    
    value = value.padStart(3, '0'); // Garante pelo menos 3 dígitos (ex: '1' vira '001')
    
    const integerPart = value.slice(0, -2);
    const decimalPart = value.slice(-2);
    
    // Converte para um número e depois formata com separador de milhar
    const formattedInteger = parseInt(integerPart, 10).toLocaleString('pt-BR');
    input.value = `${formattedInteger},${decimalPart}`;
}

// Conversor de valor formatado para número puro
function parseCurrencyValue(value) {
    // Remove separadores de milhar (ponto) e substitui a vírgula por ponto para ter um formato float
    return parseFloat(value.replace(/\./g, '').replace(',', '.'));
}

// Lógica de preenchimento automático para os campos "Matrícula" e "Transportado"
function handleAutofill(field) {
    if (field === 'matricula') {
        const matricula = matriculaInput.value.trim();
        const nome = matriculaToNome[matricula];
        if (nome) {
            transportadoInput.value = nome;
            transportadoInput.classList.remove('error-border');
        } else {
            transportadoInput.value = '';
            showWarning('Matrícula não encontrada.');
        }
    } else if (field === 'transportado') {
        const nome = transportadoInput.value.trim().toLowerCase();
        const matricula = nomeToMatricula[nome];
        if (matricula) {
            matriculaInput.value = matricula;
            matriculaInput.classList.remove('error-border');
        } else {
            matriculaInput.value = '';
            showWarning('Transportado não encontrado.');
        }
    }
}

// Funções de Lookups e Ordenação
function rebuildTransportadosLookups(sortKey = 'nome', sortOrder = 'asc') {
    transportadosData.sort((a, b) => {
        let valA = a[sortKey];
        let valB = b[sortKey];

        if (sortOrder === 'asc') {
            return valA.localeCompare(valB, undefined, { numeric: true });
        } else {
            return valB.localeCompare(valA, undefined, { numeric: true });
        }
    });

    matriculaToNome = {};
    nomeToMatricula = {};
    transportadosData.forEach(item => {
        matriculaToNome[item.matricula] = item.nome;
        nomeToMatricula[item.nome.toLowerCase()] = item.matricula;
    });

    renderTransportadosList();
}

function rebuildMotoristasLookups(sortOrder = 'asc') {
    motoristasData.sort((a, b) => {
        if (sortOrder === 'asc') {
            return a.nome.localeCompare(b.nome);
        } else {
            return b.nome.localeCompare(a.nome);
        }
    });
    renderMotoristasList();
    populateMotoristasDatalist();
}


// Funções de Renderização de Listas e Datalists
function renderTransportadosList() {
    const tableBody = document.querySelector('#transportados-table tbody');
    tableBody.innerHTML = '';
    transportadosData.forEach((item, index) => {
        const row = document.createElement('tr');
        row.className = 'bg-white hover:bg-gray-50 transition-colors duration-100';
        row.dataset.id = item.id;
        row.innerHTML = `
            <td class="p-4"><input type="checkbox" data-id="${item.id}" class="transportado-checkbox rounded-sm"></td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${item.matricula}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${item.nome}</td>
        `;
        tableBody.appendChild(row);
    });
    populateTransportadosDatalist();
}

function populateTransportadosDatalist() {
    const matriculaDatalist = document.getElementById('transportados-matricula-list');
    const nomeDatalist = document.getElementById('transportados-nome-list');
    
    matriculaDatalist.innerHTML = '';
    nomeDatalist.innerHTML = '';

    transportadosData.forEach(item => {
        const matriculaOption = document.createElement('option');
        matriculaOption.value = item.matricula;
        matriculaDatalist.appendChild(matriculaOption);
        
        const nomeOption = document.createElement('option');
        nomeOption.value = item.nome;
        nomeDatalist.appendChild(nomeOption);
    });
}

function renderMotoristasList() {
    const tableBody = document.querySelector('#motoristas-table tbody');
    tableBody.innerHTML = '';
    motoristasData.forEach((item, index) => {
        const row = document.createElement('tr');
        row.className = 'bg-white hover:bg-gray-50 transition-colors duration-100';
        row.dataset.id = item.id;
        row.innerHTML = `
            <td class="p-4"><input type="checkbox" data-id="${item.id}" class="motorista-checkbox rounded-sm"></td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${item.nome}</td>
        `;
        tableBody.appendChild(row);
    });
    populateMotoristasDatalist();
}

function populateMotoristasDatalist() {
    const motoristaDatalist = document.getElementById('motoristas-list');
    motoristaDatalist.innerHTML = '';
    motoristasData.forEach(item => {
        const option = document.createElement('option');
        option.value = item.nome;
        motoristaDatalist.appendChild(option);
    });
}


// Listeners do Firestore para dados em tempo real
function startFirestoreListeners() {
    // Listener para Transportados
    const transportadosRef = collection(db, 'artifacts', globalAppId, 'public', 'data', 'transportados');
    onSnapshot(transportadosRef, (snapshot) => {
        transportadosData = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        rebuildTransportadosLookups();
    });

    // Listener para Motoristas
    const motoristasRef = collection(db, 'artifacts', globalAppId, 'public', 'data', 'motoristas');
    onSnapshot(motoristasRef, (snapshot) => {
        motoristasData = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        rebuildMotoristasLookups();
    });
}


// --- Event Listeners ---

// Inicialização e autenticação
window.onload = () => {
    // Vincula os eventos blur (ao sair do campo) para a lógica de preenchimento automático
    matriculaInput.addEventListener('blur', () => handleAutofill('matricula'));
    transportadoInput.addEventListener('blur', () => handleAutofill('transportado'));
    
    // Adiciona event listeners para os campos de valores
    valorInput.addEventListener('input', () => formatCurrencyInput(valorInput));
    valorExtraInput.addEventListener('input', () => formatCurrencyInput(valorExtraInput));

    // Lógica para limpar campo ao focar e preencher com '0,00' ao sair
    valorInput.addEventListener('focus', () => { if (valorInput.value === '0,00') valorInput.value = ''; });
    valorExtraInput.addEventListener('focus', () => { if (valorExtraInput.value === '0,00') valorExtraInput.value = ''; });
    valorInput.addEventListener('blur', () => { if (valorInput.value === '') valorInput.value = '0,00'; });
    valorExtraInput.addEventListener('blur', () => { if (valorExtraInput.value === '') valorExtraInput.value = '0,00'; });

    onAuthStateChanged(auth, async (user) => {
        if (user) {
            globalUserId = user.uid;
            userIdDisplay.innerText = `ID do Usuário: ${globalUserId}`;
            // Sincroniza os dados do Firestore
            startFirestoreListeners();
        } else {
            // Oculta a página principal se o usuário não estiver autenticado
            loginPage.classList.remove('hidden');
            appPage.classList.add('hidden');
        }
    });

    // O login anônimo é para acesso ao Firestore, independente do seu formulário
    signInAnonymously(auth).catch((error) => {
        console.error("Erro ao fazer login anônimo:", error);
    });
    
    // Inicializa os campos de valor com '0,00'
    valorInput.value = '0,00';
    valorExtraInput.value = '0,00';

    // Evento para fechar o modal de aviso
    closeModalButton.addEventListener('click', hideWarning);
};

// Autenticação de Login
loginForm.addEventListener('submit', function(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    const foundUser = users.find(user => user.username === username && user.password === password);

    if (foundUser) {
        loginPage.classList.add('hidden');
        appPage.classList.remove('hidden');
    } else {
        loginMessage.classList.remove('hidden');
    }
});

// Logout
logoutButton.addEventListener('click', () => {
    signOut(auth);
});

// Evento de envio do formulário de corrida
document.getElementById('form-corrida').addEventListener('submit', async function(event) {
    event.preventDefault();

    const form = event.target;
    const requiredFields = [
        'motorista', 'matricula', 'transportado', 'solicitante', 'data',
        'origem', 'partida', 'destino', 'chegada',
        'valor'
    ];

    let isFormValid = true;
    for (const field of requiredFields) {
        const input = form[field];
        
        // Verifica se o campo está vazio, ignorando '0,00' apenas para 'valor' se for o caso, mas o trim() já trata.
        if (input.value.trim() === '' || (field === 'valor' && input.value.trim() === '0,00')) {
            isFormValid = false;
            input.classList.add('error-border');
        } else {
            input.classList.remove('error-border');
        }
    }

    if (!isFormValid) {
        showWarning('Por favor, preencha todos os campos obrigatórios.');
        return;
    }

    const newEntry = {
        motorista: form['motorista'].value,
        matricula: form['matricula'].value,
        transportado: form['transportado'].value,
        solicitante: form['solicitante'].value,
        data: form['data'].value,
        origem: form['origem'].value,
        partida: form['partida'].value,
        destino: form['destino'].value,
        chegada: form['chegada'].value,
        valor: parseCurrencyValue(form['valor'].value),
        valorExtra: form['valor-extra'].value && form['valor-extra'].value !== '0,00' ?
            parseCurrencyValue(form['valor-extra'].value) : null,
        observacao: form['observacao'].value
    };

    const lancamentosRef = collection(db, 'artifacts', globalAppId, 'public', 'data', 'lancamentos');
    await addDoc(lancamentosRef, newEntry);

    showWarning('Lançamento salvo com sucesso!');
    form.reset();
    document.getElementById('valor').value = '0,00';
    document.getElementById('valor-extra').value = '0,00';
});


// Evento de navegação com a tecla "Enter"
const inputs = document.querySelectorAll('#form-corrida input:not([readonly]), #form-corrida textarea');
inputs.forEach(input => {
    input.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            event.preventDefault();

            const currentInputIndex = Array.from(inputs).indexOf(this);
            let nextInput = null;

            // Tenta focar no próximo campo vazio
            for (let i = currentInputIndex + 1; i < inputs.length; i++) {
                if (inputs[i].value.trim() === '') {
                    nextInput = inputs[i];
                    break;
                }
            }

            // Se não encontrou um próximo campo vazio, volta para o primeiro campo vazio
            if (!nextInput) {
                for (let i = 0; i < inputs.length; i++) {
                    if (inputs[i].value.trim() === '') {
                        nextInput = inputs[i];
                        break;
                    }
                }
            }

            if (nextInput) {
                nextInput.focus();
            }
        }
    });
});


// Funções e Eventos do Modal de Transportados
document.getElementById('open-transportados-modal').addEventListener('click', function() {
    document.getElementById('transportados-modal').classList.remove('hidden');
});

document.getElementById('close-transportados-modal').addEventListener('click', function() {
    document.getElementById('transportados-modal').classList.add('hidden');
    document.getElementById('selectAllTransportados').checked = false;
    document.querySelectorAll('.transportado-checkbox').forEach(cb => cb.checked = false);
});

document.getElementById('add-transportado').addEventListener('click', async function() {
    const newMatriculaInput = document.getElementById('new-matricula');
    const newNomeInput = document.getElementById('new-nome');
    const newMatricula = newMatriculaInput.value.trim();
    const newNome = newNomeInput.value.trim();

    if (newMatricula && newNome) 
    {
        const existing = transportadosData.find(item => item.matricula === newMatricula || item.nome.toLowerCase() === newNome.toLowerCase());
        if (existing) {
            showWarning('A matrícula ou o nome já existe.');
        } else {
            const transportadosRef = collection(db, 'artifacts', globalAppId, 'public', 'data', 'transportados');
            await addDoc(transportadosRef, { matricula: newMatricula, nome: newNome });
            newMatriculaInput.value = '';
            newNomeInput.value = '';
            showWarning('Transportado adicionado com sucesso!');
        }
    } else {
        showWarning('Por favor, preencha a matrícula e o nome.');
    }
});

document.getElementById('delete-selected-transportados').addEventListener('click', async function() {
    const checkboxes = document.querySelectorAll('#transportados-table .transportado-checkbox:checked');
    if (checkboxes.length === 0) {
        showWarning('Selecione pelo menos um transportado para excluir.');
        return;
    }

    const promises = Array.from(checkboxes).map(cb => {
        const transportadoDocRef = doc(db, 'artifacts', globalAppId, 'public', 'data', 'transportados', cb.dataset.id);
        return deleteDoc(transportadoDocRef);
    });
    await Promise.all(promises);
    showWarning(`${checkboxes.length} transportados excluídos com sucesso!`);
});

document.getElementById('sort-transportados-key').addEventListener('change', function() {
    const sortKey = this.value;
    const sortOrder = document.getElementById('sort-transportados-order').value;
    rebuildTransportadosLookups(sortKey, sortOrder);
});

document.getElementById('sort-transportados-order').addEventListener('change', function() {
    const sortKey = document.getElementById('sort-transportados-key').value;
    const sortOrder = this.value;
    rebuildTransportadosLookups(sortKey, sortOrder);
});

document.getElementById('transportados-table').addEventListener('change', function(event) {
    if (event.target.id === 'selectAllTransportados') {
        const checkboxes = document.querySelectorAll('.transportado-checkbox');
        checkboxes.forEach(checkbox => checkbox.checked = event.target.checked);
    }
});

document.getElementById('transportados-table').addEventListener('click', function(event) {
    const row = event.target.closest('tr');
    if (row) {
        const checkbox = row.querySelector('.transportado-checkbox');
        if (checkbox && event.target !== checkbox && event.target.type !== 'checkbox') {
            checkbox.checked = !checkbox.checked;
        }
    }
});


// Funções e Eventos do Modal de Motoristas
document.getElementById('open-motoristas-modal').addEventListener('click', function() {
    document.getElementById('motoristas-modal').classList.remove('hidden');
});

document.getElementById('close-motoristas-modal').addEventListener('click', function() {
    document.getElementById('motoristas-modal').classList.add('hidden');
    document.getElementById('selectAllMotoristas').checked = false;
    document.querySelectorAll('.motorista-checkbox').forEach(cb => cb.checked = false);
});

document.getElementById('add-motorista').addEventListener('click', async function() {
    const newNomeInput = document.getElementById('new-motorista-nome');
    const newNome = newNomeInput.value.trim();

    if (newNome) {
        const existing = motoristasData.find(item => item.nome.toLowerCase() === newNome.toLowerCase());
        if (existing) {
            showWarning('O nome do motorista já existe.');
        } else {
            const motoristasRef = collection(db, 'artifacts', globalAppId, 'public', 'data', 'motoristas');
            await addDoc(motoristasRef, { nome: newNome });
            newNomeInput.value = '';
            showWarning('Motorista adicionado com sucesso!');
        }
    } else {
        showWarning('Por favor, preencha o nome do motorista.');
    }
});

document.getElementById('delete-selected-motoristas').addEventListener('click', async function() {
    const checkboxes = document.querySelectorAll('#motoristas-table .motorista-checkbox:checked');
    if (checkboxes.length === 0) {
        showWarning('Selecione pelo menos um motorista para excluir.');
        return;
    }

    const promises = Array.from(checkboxes).map(cb => {
        const motoristaDocRef = doc(db, 'artifacts', globalAppId, 'public', 'data', 'motoristas', cb.dataset.id);
        return deleteDoc(motoristaDocRef);
    });
    await Promise.all(promises);
    showWarning(`${checkboxes.length} motoristas excluídos com sucesso!`);
});

document.getElementById('sort-motoristas-order').addEventListener('change', function() {
    const sortOrder = this.value;
    rebuildMotoristasLookups(sortOrder);
});

document.getElementById('motoristas-table').addEventListener('change', function(event) {
    if (event.target.id === 'selectAllMotoristas') {
        const checkboxes = document.querySelectorAll('.motorista-checkbox');
        checkboxes.forEach(checkbox => checkbox.checked = event.target.checked);
    }
});

document.getElementById('motoristas-table').addEventListener('click', function(event) {
    const row = event.target.closest('tr');
    if (row) {
        const checkbox = row.querySelector('.motorista-checkbox');
        if (checkbox && event.target !== checkbox && event.target.type !== 'checkbox') {
            checkbox.checked = !checkbox.checked;
        }
    }
});


// Evento do botão de download CSV
document.getElementById('download-csv').addEventListener('click', async function() {
    const startDate = document.getElementById('start-date').value;
    const endDate = document.getElementById('end-date').value;

    if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
        showWarning('A data de início não pode ser posterior à data de fim.');
        return;
    }

    const lancamentosRef = collection(db, 'artifacts', globalAppId, 'public', 'data', 'lancamentos');

    // Monta filtros
    let filtros = [];
    if (startDate) filtros.push(where('data', '>=', startDate));
    if (endDate) filtros.push(where('data', '<=', endDate));

    // Monta query final
    let q = filtros.length > 0 ? query(lancamentosRef, ...filtros) : lancamentosRef;

    // Busca os dados
    try {
        const snapshot = await getDocs(q);
        const allData = snapshot.docs.map(doc => doc.data());

        console.log("Dados recebidos do Firestore:", allData); // pra debug

        if (allData.length === 0) {
            showWarning('Nenhum dado encontrado para este período.');
            return;
        }

        // Gerar CSV
        const bom = '\uFEFF';
        const headers = Object.keys(allData[0]);

        const rows = allData.map(obj => headers.map(key => {
            let value = obj[key] ?? '';
            if (key === 'valor' || key === 'valorExtra') {
                // Formatação para R$ XX,XX
                value = `R$ ${parseFloat(value).toFixed(2).replace('.', ',')}`;
            }
            // Envolve o valor em aspas e duplica as aspas internas
            return `"${value.toString().replace(/"/g, '""')}"`;
        }).join(';'));

        const csvContent = `${headers.join(';')}\n${rows.join('\n')}`;
        const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;

        const now = new Date();
        const dateString = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}-${now.getDate().toString().padStart(2, '0')}_${now.getHours().toString().padStart(2, '0')}-${now.getMinutes().toString().padStart(2, '0')}-${now.getSeconds().toString().padStart(2, '0')}`;
        link.download = `lancamentos_de_corridas_${dateString}.csv`;

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        showWarning('Relatório CSV baixado com sucesso!');
    } catch (err) {
        console.error("Erro ao buscar dados:", err);
        showWarning('Erro ao gerar relatório. Veja o console para mais detalhes.');
    }
});
