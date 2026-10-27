import api from './api';

export interface QuestionBankFilterParams {
  bankName?: string;
  subjectId?: number;
  departmentId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface QuestionFilterParams {
  bankId?: number;
  questionType?: string;
  difficulty?: string;
  subjectId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export const questionBankService = {
  // Question Banks
  getAllBanks: async (params?: QuestionBankFilterParams) => {
    const res = await api.get('/v1/question-banks', { params });
    return res.data;
  },

  getBankById: async (id: string | number) => {
    const res = await api.get(`/v1/question-banks/${id}`);
    return res.data;
  },

  createBank: async (payload: { bankName: string; description?: string; subjectId: number; departmentId: number }) => {
    const res = await api.post('/v1/question-banks', payload);
    return res.data;
  },

  updateBank: async (id: string | number, payload: any) => {
    const res = await api.put(`/v1/question-banks/${id}`, payload);
    return res.data;
  },

  patchBank: async (id: string | number, payload: any) => {
    const res = await api.patch(`/v1/question-banks/${id}`, payload);
    return res.data;
  },

  deleteBank: async (id: string | number) => {
    const res = await api.delete(`/v1/question-banks/${id}`);
    return res.data;
  },

  // Questions
  getAllQuestions: async (params?: QuestionFilterParams) => {
    const res = await api.get('/v1/questions', { params });
    return res.data;
  },

  getQuestionById: async (id: string | number) => {
    const res = await api.get(`/v1/questions/${id}`);
    return res.data;
  },

  createQuestion: async (payload: any) => {
    const res = await api.post('/v1/questions', payload);
    return res.data;
  },

  updateQuestion: async (id: string | number, payload: any) => {
    const res = await api.put(`/v1/questions/${id}`, payload);
    return res.data;
  },

  deleteQuestion: async (id: string | number) => {
    const res = await api.delete(`/v1/questions/${id}`);
    return res.data;
  },
};
