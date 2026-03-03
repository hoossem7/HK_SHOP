export interface User {
  id?: number;
  username: string;
  email?: string;
  roles?: string[]; // ex: ['USER'] ou ['SOCIETE']
  // ajoute d'autres attributs côté backend (phone, company, etc.)
}
