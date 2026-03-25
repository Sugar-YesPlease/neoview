import axios from 'axios';
import { API_BASE } from '../config';

const api = axios.create({
	baseURL: API_BASE,
	timeout: 15000,
});

api.interceptors.request.use((config) => {
	const token = sessionStorage.getItem('token');
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

export async function register(payload) {
	const { data } = await api.post('/api/auth/register', payload);
	return data;
}

export async function login(payload) {
	const { data } = await api.post('/api/auth/login', payload);
	return data;
}

export async function createMeeting(payload) {
	const { data } = await api.post('/api/meetings', payload);
	return data;
}
